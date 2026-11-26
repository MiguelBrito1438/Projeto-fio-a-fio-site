/* =========================================================
   Fio a Fio — gestão de pedidos têxteis
   Sem window.FIO_A_FIO_API_BASE o site roda em modo de demonstração,
   com dados fictícios gerados em relação à data de hoje.
   ========================================================= */

const API_BASE_URL = (window.FIO_A_FIO_API_BASE || "").replace(/\/$/, "");
const USE_DEMO_DATA = !API_BASE_URL;
const CURRENT_USER = "Maria Almeida";
const CURRENT_PROFILE = "OPERADOR";
const HISTORY_ORDER_LIMIT = 30; // evita centenas de requisições de histórico

// Ordem do fluxo normal (igual ao enum StatusPedido do backend).
const FLOW = ["PENDENTE", "EM_PRODUCAO", "EM_PREPARACAO", "AGUARDANDO_COLETA", "EM_TRANSPORTE", "ENTREGUE"];
const STATUS = {
  PENDENTE: { label: "Pendente", icon: "clock", color: "--st-pending" },
  EM_PRODUCAO: { label: "Em produção", icon: "scissors", color: "--st-production" },
  EM_PREPARACAO: { label: "Em preparação", icon: "box", color: "--st-prep" },
  AGUARDANDO_COLETA: { label: "Aguardando coleta", icon: "flag", color: "--st-pickup" },
  EM_TRANSPORTE: { label: "Em transporte", icon: "truck", color: "--st-transit" },
  ENTREGUE: { label: "Entregue", icon: "check", color: "--st-delivered" },
  CANCELADO: { label: "Cancelado", icon: "x", color: "--st-cancel" }
};
const ADVANCE_TEXT = {
  EM_PRODUCAO: "Iniciar produção",
  EM_PREPARACAO: "Enviar para preparação",
  AGUARDANDO_COLETA: "Marcar como pronto para coleta",
  EM_TRANSPORTE: "Registrar saída para transporte",
  ENTREGUE: "Confirmar entrega"
};
const ADVANCE_DESCRIPTION = {
  EM_PRODUCAO: "Início da produção",
  EM_PREPARACAO: "Peças em acabamento e embalagem",
  AGUARDANDO_COLETA: "Pedido embalado, aguardando coleta",
  EM_TRANSPORTE: "Carga saiu para entrega",
  ENTREGUE: "Entrega confirmada"
};
const PAGE_NAMES = { painel: "Painel geral", cadastro: "Novo pedido", consulta: "Consultar pedidos", historico: "Histórico" };
const isOpenStatus = status => status !== "ENTREGUE" && status !== "CANCELADO";

const $ = selector => document.querySelector(selector);
const $$ = selector => [...document.querySelectorAll(selector)];

let orders = [];
let historyEntries = [];
let dataState = USE_DEMO_DATA ? "demo" : "loading"; // demo | loading | live | error
let toastTimer;
let drawerOrderKey = null;

/* ---------------- utilidades de data ---------------- */
const pad = n => String(n).padStart(2, "0");
const toISODate = date => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
const todayISO = () => toISODate(new Date()); // data local (toISOString usa UTC e erra à noite no Brasil)
function addDays(base, days) { const d = new Date(base); d.setDate(d.getDate() + days); return d; }
function parseDate(iso) {
  if (!iso) return null;
  const d = new Date(`${String(iso).slice(0, 10)}T12:00:00`);
  return Number.isNaN(d.getTime()) ? null : d;
}
function daysUntil(iso) {
  const d = parseDate(iso);
  if (!d) return null;
  return Math.round((d - parseDate(todayISO())) / 86400000);
}
const fmt = (options, date) => new Intl.DateTimeFormat("pt-BR", options).format(date).replace(".", "");
function formatDate(iso) { const d = parseDate(iso); return d ? fmt({ day: "2-digit", month: "short" }, d) : "—"; }
function formatLongDate(iso) { const d = parseDate(iso); return d ? fmt({ day: "2-digit", month: "long", year: "numeric" }, d) : "—"; }
function formatDateTime(value) {
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return "";
  const diff = daysUntil(toISODate(d));
  const time = fmt({ hour: "2-digit", minute: "2-digit" }, d);
  if (diff === 0) return `Hoje, ${time}`;
  if (diff === -1) return `Ontem, ${time}`;
  return `${fmt({ day: "2-digit", month: "short" }, d)}, ${time}`;
}
function relativeDue(order) {
  const days = daysUntil(order.dueDate);
  if (days === null) return { text: "", tone: "" };
  if (!isOpenStatus(order.status)) return { text: order.status === "ENTREGUE" ? "entregue" : "cancelado", tone: "" };
  if (days < 0) return { text: `${Math.abs(days)} ${Math.abs(days) === 1 ? "dia" : "dias"} de atraso`, tone: "is-late" };
  if (days === 0) return { text: "vence hoje", tone: "is-soon" };
  if (days === 1) return { text: "vence amanhã", tone: "is-soon" };
  return { text: `em ${days} dias`, tone: days <= 3 ? "is-soon" : "" };
}

/* ---------------- utilidades de HTML ---------------- */
function escapeHtml(value = "") {
  return String(value ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
}
const icon = (name, extra = "") => `<svg class="icon ${extra}" aria-hidden="true"><use href="#i-${name}"/></svg>`;
const initials = name => String(name || "?").trim().split(/\s+/).filter(w => w.length > 2 || /^[A-ZÀ-Ú]/.test(w)).slice(0, 2).map(w => w[0]).join("").toUpperCase() || "?";
function avatarTone(name) { let h = 0; for (const ch of String(name)) h = (h * 31 + ch.charCodeAt(0)) >>> 0; return `av-${h % 5}`; }
const avatar = name => `<span class="avatar ${avatarTone(name)}" aria-hidden="true">${escapeHtml(initials(name))}</span>`;
const statusBadge = status => `<span class="status st-${escapeHtml(status)}">${escapeHtml(STATUS[status]?.label || status || "Sem status")}</span>`;
const pluralize = (n, one, many) => `${n} ${n === 1 ? one : many}`;

/* ---------------- camada da API ---------------- */
async function apiRequest(path, { method = "GET", body, user = false } = {}) {
  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers: {
        Accept: "application/json",
        ...(body ? { "Content-Type": "application/json" } : {}),
        ...(user ? { "X-Usuario": CURRENT_USER, "X-Perfil": CURRENT_PROFILE } : {})
      },
      ...(body ? { body: JSON.stringify(body) } : {})
    });
  } catch {
    throw new Error("Sem conexão com o servidor. Verifique a internet e tente novamente.");
  }
  if (!response.ok) {
    const detail = await response.json().catch(() => null);
    const fields = detail?.campos ? Object.values(detail.campos).join(" ") : "";
    throw new Error([detail?.mensagem || detail?.message, fields].filter(Boolean).join(" ") || `Não foi possível concluir a solicitação (erro ${response.status}).`);
  }
  if (response.status === 204) return null;
  return response.json();
}

const orderApi = {
  list: () => apiRequest("/pedidos?page=0&size=100&sort=id,desc"),
  clients: name => apiRequest(`/clientes?nome=${encodeURIComponent(name)}`),
  createClient: payload => apiRequest("/clientes", { method: "POST", body: payload }),
  create: payload => apiRequest("/pedidos", { method: "POST", body: payload }),
  history: id => apiRequest(`/pedidos/${id}/historico`),
  updateStatus: (id, novoStatus, descricao = "", localizacao = "") =>
    apiRequest(`/pedidos/${id}/status`, { method: "PATCH", body: { novoStatus, descricao, localizacao }, user: true })
};

/* ---------------- normalização (API e demo usam o mesmo formato) ---------------- */
function normalizeOrder(o) {
  const items = (o.itens || []).map(item => ({ description: item.descricao, quantity: Number(item.quantidade) || 0, unit: item.unidade || "peças" }));
  const status = STATUS[o.status] ? o.status : "PENDENTE";
  const notes = o.observacoes || "";
  const late = typeof o.atrasado === "boolean" ? o.atrasado : isOpenStatus(status) && (daysUntil(o.previsaoEntrega) ?? 0) < 0;
  return {
    key: String(o.codigo || o.id),
    apiId: o.id,
    code: o.codigo || `#${o.id}`,
    client: o.cliente?.nome || "Cliente sem nome",
    clientPhone: o.cliente?.telefone || "",
    items,
    product: items.length ? items[0].description + (items.length > 1 ? ` + ${items.length - 1}` : "") : "—",
    quantity: items.reduce((sum, item) => sum + item.quantity, 0),
    orderDate: o.dataPedido,
    dueDate: o.previsaoEntrega,
    status,
    origin: o.origem || "",
    destination: o.destino || "",
    region: o.regiaoDestino || "",
    responsible: o.responsavel || "",
    notes,
    priority: /prioridade:\s*alta/i.test(notes) ? "Alta" : "Normal",
    lastLocation: o.ultimaLocalizacao || "",
    updatedAt: o.atualizadoEm,
    late,
    searchText: [o.codigo, o.cliente?.nome, ...items.map(i => i.description), o.destino].join(" ").toLocaleLowerCase("pt-BR")
  };
}

function normalizeHistory(entry, order) {
  const status = entry.statusNovo;
  const iconByType = { CADASTRO: "plus", ATUALIZACAO_LOCALIZACAO: "pin", CONFIRMACAO_ENTREGA: "check", CANCELAMENTO: "x", CORRECAO_EXCEPCIONAL: "refresh", EDICAO_DADOS: "refresh" };
  return {
    orderKey: order.key,
    code: order.code,
    client: order.client,
    type: entry.tipo,
    status,
    icon: iconByType[entry.tipo] || STATUS[status]?.icon || "refresh",
    date: new Date(entry.dataHora),
    author: entry.responsavel || "",
    location: entry.localizacao || "",
    description: entry.descricao || (entry.tipo === "CADASTRO" ? "Pedido cadastrado." : `Status alterado para ${STATUS[status]?.label || status}.`)
  };
}

/* ---------------- dados de demonstração ---------------- */
function buildDemoData() {
  const today = new Date();
  const day = offset => toISODate(addDays(today, offset));
  const clients = {
    flor: { id: 1, nome: "Boutique Flor de Mandacaru", telefone: "(81) 99812-4410", cidade: "Recife" },
    armazem: { id: 2, nome: "Armazém da Moda", telefone: "(87) 99654-2201", cidade: "Garanhuns" },
    rendas: { id: 3, nome: "Casa das Rendas", telefone: "(81) 98877-1020", cidade: "Olinda" },
    estilo: { id: 4, nome: "Estilo Agreste", telefone: "(81) 99701-3344", cidade: "Caruaru" },
    raiz: { id: 5, nome: "Moda Raiz", telefone: "(87) 98123-5566", cidade: "Petrolina" },
    sol: { id: 6, nome: "Ateliê Sol Nascente", telefone: "(83) 99230-7788", cidade: "João Pessoa" },
    lojaM: { id: 7, nome: "Loja Mandacaru", telefone: "(87) 99540-1122", cidade: "Arcoverde" }
  };
  const raw = [
    [112, "flor", "Vestido midi estampado", 50, -12, 3, "EM_PRODUCAO", "Caruaru", "Recife", "Região Metropolitana", "Prioridade: alta\nViscose estampada, grade P ao GG."],
    [111, "armazem", "Conjunto de linho", 30, -15, 1, "AGUARDANDO_COLETA", "Caruaru", "Garanhuns", "Agreste Meridional", ""],
    [110, "rendas", "Blusa de renda renascença", 24, -3, 16, "PENDENTE", "Caruaru", "Olinda", "Região Metropolitana", "Aguardando chegada da renda."],
    [109, "estilo", "Saia longa floral", 40, -20, -2, "EM_TRANSPORTE", "Caruaru", "Gravatá", "Agreste Central", ""],
    [108, "raiz", "Camisa de algodão cru", 60, -18, 5, "EM_PREPARACAO", "Caruaru", "Petrolina", "Sertão do São Francisco", ""],
    [107, "armazem", "Calça jeans reta", 80, -14, -1, "EM_PRODUCAO", "Toritama", "Garanhuns", "Agreste Meridional", "Prioridade: alta\nLavagem stone."],
    [106, "flor", "Conjunto cropped e short", 35, -25, -6, "ENTREGUE", "Caruaru", "Recife", "Região Metropolitana", ""],
    [105, "sol", "Bermuda de sarja", 45, -30, -10, "ENTREGUE", "Santa Cruz do Capibaribe", "João Pessoa", "Paraíba", ""],
    [104, "lojaM", "Kit fardamento escolar", 120, -22, -12, "CANCELADO", "Caruaru", "Arcoverde", "Sertão do Moxotó", ""],
    [103, "rendas", "Toalha de mesa bordada", 18, -1, 21, "PENDENTE", "Caruaru", "Olinda", "Região Metropolitana", ""]
  ];
  const authors = ["Maria Almeida", "João Batista", "Rita Ferreira"];
  const descriptions = {
    EM_PRODUCAO: "Corte iniciado no ateliê.",
    EM_PREPARACAO: "Costura concluída; peças em acabamento e embalagem.",
    AGUARDANDO_COLETA: "Pedido embalado, aguardando a transportadora.",
    ENTREGUE: "Entrega confirmada pelo cliente."
  };
  const demoOrders = [];
  const demoHistory = [];
  raw.forEach(([n, clientKey, product, qty, orderOffset, dueOffset, status, origin, destination, region, notes], index) => {
    const id = n;
    const apiOrder = {
      id, codigo: `PED-2026-${String(n).padStart(4, "0")}`, cliente: clients[clientKey], responsavel: authors[index % 3],
      origem: origin, destino: destination, regiaoOrigem: "Agreste", regiaoDestino: region,
      itens: [{ descricao: product, quantidade: qty, unidade: "peças" }],
      dataPedido: day(orderOffset), previsaoEntrega: day(dueOffset), status, observacoes: notes,
      ultimaLocalizacao: status === "EM_TRANSPORTE" ? "BR-232, próximo a Bezerros" : origin
    };
    const order = normalizeOrder(apiOrder);
    demoOrders.push(order);

    const target = status === "CANCELADO" ? 1 : FLOW.indexOf(status);
    const end = status === "ENTREGUE" ? Math.min(dueOffset - 1, -1) : 0;
    const steps = status === "CANCELADO" ? ["PENDENTE", "EM_PRODUCAO", "CANCELADO"] : FLOW.slice(0, target + 1);
    steps.forEach((step, i) => {
      const offset = steps.length === 1 ? orderOffset : Math.round(orderOffset + (i * (end - orderOffset)) / (steps.length - 1));
      const when = addDays(today, offset);
      when.setHours(8 + ((i * 3 + index) % 4), (i * 17 + index * 7) % 60, 0, 0);
      const type = i === 0 ? "CADASTRO" : step === "ENTREGUE" ? "CONFIRMACAO_ENTREGA" : step === "CANCELADO" ? "CANCELAMENTO" : "ALTERACAO_STATUS";
      const description = i === 0 ? "Pedido cadastrado." : step === "EM_TRANSPORTE" ? `Carga saiu de ${origin} com destino a ${destination}.` : step === "CANCELADO" ? "Cliente desistiu do pedido; produção interrompida." : descriptions[step];
      demoHistory.push(normalizeHistory({ statusNovo: step, tipo: type, dataHora: when.toISOString(), responsavel: authors[(index + i) % 3], localizacao: origin, descricao: description }, order));
    });
  });
  return { demoOrders, demoHistory };
}

/* ---------------- indicadores ---------------- */
function computeStats() {
  const open = orders.filter(o => isOpenStatus(o.status));
  return {
    open,
    progress: open.filter(o => o.status !== "PENDENTE").length,
    pending: open.filter(o => o.status === "PENDENTE").length,
    week: open.filter(o => { const d = daysUntil(o.dueDate); return d !== null && d >= 0 && d <= 7; }).length,
    late: open.filter(o => o.late).length
  };
}

/* ---------------- renderização: painel ---------------- */
function skeletonRows(cols, rows = 4) {
  return Array.from({ length: rows }, () => `<tr>${Array.from({ length: cols }, () => `<td><span class="skeleton"></span></td>`).join("")}</tr>`).join("");
}
function emptyRow(cols, title, text = "") {
  return `<tr><td class="empty-state" colspan="${cols}"><strong>${escapeHtml(title)}</strong>${escapeHtml(text)}</td></tr>`;
}
function dueCell(order) {
  const rel = relativeDue(order);
  return `<span class="due ${rel.tone}">${formatDate(order.dueDate)}<small>${escapeHtml(rel.text)}</small></span>`;
}

function renderDashboard() {
  const hour = new Date().getHours();
  $("#greeting").textContent = hour < 12 ? "Bom dia" : hour < 18 ? "Boa tarde" : "Boa noite";
  const tbody = $("#dashboard-orders");

  if (dataState === "loading") {
    tbody.innerHTML = skeletonRows(4);
    $("#dashboard-summary").textContent = "Carregando o resumo da produção…";
    return;
  }
  if (dataState === "error") {
    tbody.innerHTML = emptyRow(4, "Não foi possível carregar os pedidos.", " Tente recarregar a página.");
    $("#dashboard-summary").textContent = "Não conseguimos falar com o servidor agora.";
    $("#delivery-list").innerHTML = "";
    return;
  }

  const stats = computeStats();
  $("#stat-progress").textContent = stats.progress;
  $("#stat-pending").textContent = stats.pending;
  $("#stat-week").textContent = stats.week;
  $("#stat-late").textContent = stats.late;
  $("#stat-late-card").classList.toggle("has-late", stats.late > 0);
  const navCount = $("#nav-open-count");
  navCount.textContent = stats.open.length;
  navCount.hidden = !stats.open.length;

  $("#dashboard-summary").textContent = !orders.length
    ? "Nenhum pedido cadastrado ainda. Que tal começar pelo primeiro?"
    : stats.late
      ? `${pluralize(stats.open.length, "pedido aberto", "pedidos abertos")}, ${pluralize(stats.late, "com prazo vencido", "com prazo vencido")}. Vale dar uma olhada.`
      : `${pluralize(stats.open.length, "pedido aberto", "pedidos abertos")} e nenhum atraso. A produção está em dia.`;

  const recent = orders.slice(0, 5);
  tbody.innerHTML = recent.length ? recent.map(order => `
    <tr data-order="${escapeHtml(order.key)}" tabindex="0" aria-label="Abrir ${escapeHtml(order.code)}">
      <td data-label="Pedido"><span class="order-code">${escapeHtml(order.code)}</span></td>
      <td data-label="Cliente"><span class="client-cell">${avatar(order.client)}<strong>${escapeHtml(order.client)}</strong></span></td>
      <td data-label="Entrega">${dueCell(order)}</td>
      <td data-label="Status">${statusBadge(order.status)}</td>
    </tr>`).join("") : emptyRow(4, "Nenhum pedido ainda.", " Cadastre o primeiro em “Novo pedido”.");

  // Fluxo da produção (donut calculado a partir dos dados)
  const counts = FLOW.slice(0, 5).map(status => ({ status, count: stats.open.filter(o => o.status === status).length }));
  const total = stats.open.length;
  const styles = getComputedStyle(document.documentElement);
  let acc = 0;
  const segments = counts.filter(c => c.count).map(c => {
    const start = (acc / total) * 100; acc += c.count;
    const color = styles.getPropertyValue(STATUS[c.status].color).trim();
    return `${color} ${start}% ${(acc / total) * 100}%`;
  });
  $("#donut").style.setProperty("--donut", segments.length ? `conic-gradient(${segments.join(", ")})` : "");
  $("#donut").setAttribute("aria-label", `Pedidos abertos por etapa: ${counts.map(c => `${STATUS[c.status].label} ${c.count}`).join(", ")}`);
  $("#donut-total").textContent = total;
  $("#pipeline-list").innerHTML = counts.map(c => `
    <li style="--st: var(${STATUS[c.status].color})"><button type="button" class="pipeline-row" data-filter-status="${c.status}" title="Ver pedidos: ${STATUS[c.status].label}"><i></i><span>${STATUS[c.status].label}</span><strong>${c.count}</strong></button></li>`).join("");

  // Próximas entregas: atrasados primeiro, depois por prazo
  const upcoming = [...stats.open].sort((a, b) => String(a.dueDate).localeCompare(String(b.dueDate))).slice(0, 6);
  $("#delivery-list").innerHTML = upcoming.length ? upcoming.map(order => {
    const d = parseDate(order.dueDate);
    const rel = relativeDue(order);
    return `<button type="button" class="delivery-item ${rel.tone}" data-order="${escapeHtml(order.key)}">
      <span class="delivery-date"><span>${d ? fmt({ month: "short" }, d) : "—"}</span><strong>${d ? fmt({ day: "2-digit" }, d) : "—"}</strong></span>
      <span class="delivery-copy"><strong>${escapeHtml(order.client)}</strong><span>${escapeHtml(order.product)} · ${escapeHtml(order.destination)}</span><small>${escapeHtml(rel.text)} · ${escapeHtml(STATUS[order.status].label)}</small></span>
    </button>`;
  }).join("") : `<p class="empty-state"><strong>Nenhuma entrega pendente.</strong>Todos os pedidos foram concluídos.</p>`;
}

/* ---------------- renderização: consulta ---------------- */
function renderConsultation() {
  const tbody = $("#consultation-orders");
  const query = $("#order-search").value.trim().toLocaleLowerCase("pt-BR");
  const filter = $("#status-filter").value;
  const sort = $("#sort-order").value;
  $("#clear-filters").hidden = !query && !filter && sort === "recent";

  if (dataState === "loading") { tbody.innerHTML = skeletonRows(7, 6); $("#results-count").textContent = "Carregando…"; return; }
  if (dataState === "error") { tbody.innerHTML = emptyRow(7, "Não foi possível carregar os pedidos.", " Tente recarregar a página."); $("#results-count").textContent = ""; return; }

  const filtered = orders.filter(order => {
    if (query && !order.searchText.includes(query)) return false;
    if (filter === "ABERTOS") return isOpenStatus(order.status);
    if (filter === "ATRASADO") return order.late;
    return !filter || order.status === filter;
  });
  if (sort === "due") filtered.sort((a, b) => String(a.dueDate).localeCompare(String(b.dueDate)));
  if (sort === "client") filtered.sort((a, b) => a.client.localeCompare(b.client, "pt-BR"));

  tbody.innerHTML = filtered.length ? filtered.map(order => `
    <tr data-order="${escapeHtml(order.key)}">
      <td data-label="Pedido"><span class="order-code">${escapeHtml(order.code)}</span>${order.priority === "Alta" && isOpenStatus(order.status) ? ` <span class="tag tag-high">Alta</span>` : ""}</td>
      <td data-label="Cliente"><span class="client-cell">${avatar(order.client)}<strong>${escapeHtml(order.client)}</strong></span></td>
      <td data-label="Produto">${escapeHtml(order.product)}</td>
      <td data-label="Qtd." class="num">${order.quantity.toLocaleString("pt-BR")}</td>
      <td data-label="Entrega">${dueCell(order)}</td>
      <td data-label="Status">${statusBadge(order.status)}</td>
      <td data-label="Ações"><button class="row-action" type="button" data-order="${escapeHtml(order.key)}" aria-label="Ver detalhes de ${escapeHtml(order.code)}">${icon("chevron")}</button></td>
    </tr>`).join("")
    : orders.length ? emptyRow(7, "Nenhum pedido encontrado.", " Tente outra busca ou limpe os filtros.") : emptyRow(7, "Nenhum pedido cadastrado ainda.");
  $("#results-count").textContent = `${pluralize(filtered.length, "pedido encontrado", "pedidos encontrados")}${filtered.length !== orders.length ? ` de ${orders.length}` : ""}`;
}

/* ---------------- renderização: histórico ---------------- */
function renderTimeline() {
  const container = $("#timeline");
  if (dataState === "loading") { container.innerHTML = `<p class="empty-state">Carregando histórico…</p>`; return; }
  const period = $("#history-period").value;
  const limit = period === "all" ? null : addDays(new Date(), -Number(period));
  const entries = historyEntries.filter(e => !limit || e.date >= limit).sort((a, b) => b.date - a.date);
  if (!entries.length) {
    container.innerHTML = `<p class="empty-state"><strong>Nenhuma atividade neste período.</strong>${historyEntries.length ? "Experimente ampliar o período." : "As movimentações aparecem aqui assim que os pedidos forem atualizados."}</p>`;
    return;
  }
  let html = "";
  let lastDay = "";
  entries.forEach((entry, i) => {
    const dayKey = toISODate(entry.date);
    if (dayKey !== lastDay) {
      const diff = daysUntil(dayKey);
      const label = diff === 0 ? "Hoje" : diff === -1 ? "Ontem" : fmt({ weekday: "long", day: "2-digit", month: "long" }, entry.date);
      html += `<h3 class="timeline-day">${escapeHtml(label)}</h3>`;
      lastDay = dayKey;
    }
    const nextSameDay = entries[i + 1] && toISODate(entries[i + 1].date) === dayKey;
    const st = STATUS[entry.status];
    html += `<article class="timeline-entry ${nextSameDay ? "" : "is-last-of-day"}">
      <span class="timeline-icon" style="${st ? `--st: var(${st.color}); --st-bg: var(${st.color}-bg)` : ""}">${icon(entry.icon)}</span>
      <div class="timeline-copy">
        <p><button type="button" class="link" data-order="${escapeHtml(entry.orderKey)}">${escapeHtml(entry.code)}</button> · ${escapeHtml(entry.client)}</p>
        <p>${escapeHtml(entry.description)}</p>
        <small>${escapeHtml(fmt({ hour: "2-digit", minute: "2-digit" }, entry.date))}${entry.author ? ` · ${escapeHtml(entry.author)}` : ""}${entry.location ? ` · ${escapeHtml(entry.location)}` : ""}</small>
      </div>
      ${entry.status ? statusBadge(entry.status) : ""}
    </article>`;
  });
  container.innerHTML = html;
}

function renderSource() {
  const card = $("#source-card");
  const map = {
    demo: ["Modo demonstração", "Dados fictícios desta sessão. Nada é salvo no servidor.", "Dados de demonstração"],
    loading: ["Conectando…", "Buscando pedidos no servidor.", "Carregando"],
    live: ["Conectado ao servidor", "Os pedidos são salvos e compartilhados com a equipe.", "Dados do servidor"],
    error: ["Servidor indisponível", "Não foi possível carregar os dados. Recarregue a página em instantes.", "Sem conexão"]
  }[dataState];
  $("#source-title").textContent = map[0];
  $("#source-text").textContent = map[1];
  $("#data-source-label").textContent = map[2];
  card.classList.toggle("is-live", dataState === "live");
  card.classList.toggle("is-error", dataState === "error");
}

function renderClientSuggestions() {
  const names = [...new Set(orders.map(o => o.client))].sort((a, b) => a.localeCompare(b, "pt-BR"));
  $("#client-suggestions").innerHTML = names.map(name => `<option value="${escapeHtml(name)}">`).join("");
}

function renderAll() {
  renderSource(); renderDashboard(); renderConsultation(); renderTimeline(); renderClientSuggestions();
  if (drawerOrderKey && $("#order-drawer").open) renderDrawer(drawerOrderKey);
}

/* ---------------- navegação (hash) ---------------- */
function currentView() {
  const name = location.hash.replace("#", "");
  return PAGE_NAMES[name] ? name : "painel";
}
function showView(viewName, { focus = true } = {}) {
  $$(".view").forEach(view => view.classList.toggle("active", view.id === `view-${viewName}`));
  $$(".nav-link").forEach(link => {
    if (link.dataset.view === viewName) link.setAttribute("aria-current", "page"); else link.removeAttribute("aria-current");
  });
  $("#breadcrumb-current").textContent = PAGE_NAMES[viewName];
  document.title = `${PAGE_NAMES[viewName]} · Fio a Fio`;
  setSidebar(false);
  window.scrollTo({ top: 0 });
  if (focus) $("#conteudo").focus({ preventScroll: true });
}
function goTo(viewName) {
  if (location.hash === `#${viewName}`) showView(viewName); else location.hash = viewName;
}
window.addEventListener("hashchange", () => showView(currentView()));

/* ---------------- menu lateral (mobile) ---------------- */
function setSidebar(open) {
  $("#sidebar").classList.toggle("open", open);
  $("#sidebar-backdrop").hidden = !open;
  $("#mobile-menu").setAttribute("aria-expanded", String(open));
  $("#mobile-menu").setAttribute("aria-label", open ? "Fechar menu" : "Abrir menu");
  if (open) $(".nav-link[aria-current]")?.focus();
}
$("#mobile-menu").addEventListener("click", () => setSidebar(!$("#sidebar").classList.contains("open")));
$("#sidebar-backdrop").addEventListener("click", () => setSidebar(false));
document.addEventListener("keydown", event => {
  if (event.key === "Escape" && $("#sidebar").classList.contains("open")) { setSidebar(false); $("#mobile-menu").focus(); }
});

/* ---------------- detalhes do pedido ---------------- */
async function orderHistory(order) {
  if (USE_DEMO_DATA) return historyEntries.filter(e => e.orderKey === order.key);
  const list = await orderApi.history(order.apiId);
  return (list || []).map(entry => normalizeHistory(entry, order));
}

function renderDrawer(key) {
  const order = orders.find(o => o.key === key);
  if (!order) return;
  $("#drawer-title").textContent = order.code;
  $("#drawer-client").textContent = order.client;
  const currentIndex = FLOW.indexOf(order.status);
  const next = isOpenStatus(order.status) ? FLOW[currentIndex + 1] : null;
  const rel = relativeDue(order);

  const stepper = order.status === "CANCELADO" ? "" : `<ol class="stepper">${FLOW.map((status, i) => {
    const state = i < currentIndex || order.status === "ENTREGUE" ? "done" : i === currentIndex ? "current" : "";
    return `<li class="${state}"><span class="dot">${state === "done" ? icon("check") : ""}</span>${STATUS[status].label}</li>`;
  }).join("")}</ol>`;

  $("#drawer-body").innerHTML = `
    <div class="drawer-status">${statusBadge(order.status)}${order.late ? `<span class="tag tag-late">${icon("alert", "icon-sm")} ${escapeHtml(rel.text)}</span>` : ""}${order.priority === "Alta" && isOpenStatus(order.status) ? `<span class="tag tag-high">Prioridade alta</span>` : ""}</div>
    <section class="drawer-section">
      <h3>Etapas</h3>
      ${stepper || `<p class="drawer-note">Pedido cancelado. Para retomar, cadastre um novo pedido.</p>`}
      ${next ? `<div class="drawer-actions"><button class="button button-primary button-sm" type="button" data-advance="${escapeHtml(order.key)}" data-next="${next}">${icon(STATUS[next].icon)}${escapeHtml(ADVANCE_TEXT[next])}</button></div>
      <p class="drawer-note">A mudança é registrada no histórico em nome de ${escapeHtml(CURRENT_USER)}.</p>` : ""}
    </section>
    <section class="drawer-section">
      <h3>Dados do pedido</h3>
      <dl class="detail-grid">
        <div class="full"><dt>Itens</dt><dd>${order.items.map(i => `${escapeHtml(i.description)} — ${i.quantity.toLocaleString("pt-BR")} ${escapeHtml(i.unit)}`).join("<br>") || "—"}</dd></div>
        <div><dt>Data do pedido</dt><dd>${formatLongDate(order.orderDate)}</dd></div>
        <div><dt>Entrega prevista</dt><dd>${formatLongDate(order.dueDate)}</dd></div>
        <div><dt>Origem</dt><dd>${escapeHtml(order.origin || "—")}</dd></div>
        <div><dt>Destino</dt><dd>${escapeHtml(order.destination || "—")}${order.region ? ` <small>(${escapeHtml(order.region)})</small>` : ""}</dd></div>
        <div><dt>Última localização</dt><dd>${escapeHtml(order.lastLocation || "—")}</dd></div>
        <div><dt>Responsável</dt><dd>${escapeHtml(order.responsible || "—")}</dd></div>
        ${order.clientPhone ? `<div><dt>Telefone do cliente</dt><dd><a href="tel:${escapeHtml(order.clientPhone.replace(/[^\d+]/g, ""))}">${escapeHtml(order.clientPhone)}</a></dd></div>` : ""}
        ${order.notes ? `<div class="full"><dt>Observações</dt><dd>${escapeHtml(order.notes).replace(/\n/g, "<br>")}</dd></div>` : ""}
      </dl>
    </section>
    <section class="drawer-section">
      <h3>Movimentações</h3>
      <div id="drawer-history"><span class="skeleton"></span></div>
    </section>`;

  orderHistory(order).then(list => {
    if (drawerOrderKey !== key) return;
    const target = $("#drawer-history");
    if (!target) return;
    const sorted = [...list].sort((a, b) => b.date - a.date);
    target.innerHTML = sorted.length ? `<ul class="mini-timeline">${sorted.map(e => `<li style="${STATUS[e.status] ? `--st: var(${STATUS[e.status].color})` : ""}"><div><p>${escapeHtml(e.description)}</p><small>${escapeHtml(formatDateTime(e.date))}${e.author ? ` · ${escapeHtml(e.author)}` : ""}${e.location ? ` · ${escapeHtml(e.location)}` : ""}</small></div></li>`).join("")}</ul>` : `<p class="drawer-note">Nenhuma movimentação registrada.</p>`;
  }).catch(error => {
    const target = $("#drawer-history");
    if (target) target.innerHTML = `<p class="drawer-note">${escapeHtml(error.message)}</p>`;
  });
}

function openDrawer(key) {
  drawerOrderKey = key;
  renderDrawer(key);
  const drawer = $("#order-drawer");
  if (!drawer.open) drawer.showModal();
}
$("#order-drawer").addEventListener("click", event => {
  if (event.target === event.currentTarget || event.target.closest("[data-close-drawer]")) event.currentTarget.close();
});
$("#order-drawer").addEventListener("close", () => { drawerOrderKey = null; });

async function advanceOrder(key, nextStatus, button) {
  const order = orders.find(o => o.key === key);
  if (!order) return;
  button.disabled = true;
  try {
    if (USE_DEMO_DATA) {
      order.status = nextStatus;
      order.late = isOpenStatus(nextStatus) && (daysUntil(order.dueDate) ?? 0) < 0;
      if (nextStatus === "EM_TRANSPORTE") order.lastLocation = `Saindo de ${order.origin}`;
      if (nextStatus === "ENTREGUE") order.lastLocation = order.destination;
      historyEntries.push(normalizeHistory({ statusNovo: nextStatus, tipo: nextStatus === "ENTREGUE" ? "CONFIRMACAO_ENTREGA" : "ALTERACAO_STATUS", dataHora: new Date().toISOString(), responsavel: CURRENT_USER, localizacao: order.lastLocation, descricao: ADVANCE_DESCRIPTION[nextStatus] + "." }, order));
    } else {
      const updated = normalizeOrder(await orderApi.updateStatus(order.apiId, nextStatus, ADVANCE_DESCRIPTION[nextStatus], order.lastLocation || order.origin));
      Object.assign(order, updated);
      loadHistory();
    }
    renderAll();
    showToast(`${order.code}: ${STATUS[nextStatus].label.toLocaleLowerCase("pt-BR")}.`);
  } catch (error) {
    showToast(error.message, true);
    button.disabled = false;
    // O pedido pode ter sido alterado por outra pessoa: recarrega para a tela refletir o estado real.
    if (!USE_DEMO_DATA) loadOrders().then(() => { if (dataState === "live") loadHistory(); });
  }
}

/* ---------------- toast ---------------- */
function showToast(message, isError = false) {
  const toast = $("#toast");
  toast.textContent = message;
  toast.setAttribute("role", isError ? "alert" : "status");
  toast.classList.toggle("toast-error", isError);
  toast.classList.add("visible");
  window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => toast.classList.remove("visible"), isError ? 6500 : 4000);
}

/* ---------------- formulário ---------------- */
const form = $("#order-form");
let regionTouched = false;

function setFieldError(input, message) {
  const field = input.closest(".field");
  field.classList.toggle("invalid", Boolean(message));
  const error = field.querySelector(".field-error");
  if (error) error.textContent = message || "";
  input.setAttribute("aria-invalid", message ? "true" : "false");
  if (error) {
    error.id ||= `err-${input.name}`;
    if (message) input.setAttribute("aria-describedby", error.id); else input.removeAttribute("aria-describedby");
  }
}

function validateOrderForm() {
  const el = form.elements;
  const today = todayISO();
  const errors = {};
  const required = { client: "Informe o nome do cliente.", product: "Informe o produto.", origin: "Informe a cidade de origem.", destination: "Informe a cidade de destino.", destinationRegion: "Informe a região de destino.", responsible: "Informe quem é o responsável.", orderDate: "Informe a data do pedido.", dueDate: "Informe a data de entrega prevista." };
  for (const [name, message] of Object.entries(required)) if (!el[name].value.trim()) errors[name] = message;
  const qty = Number(el.quantity.value);
  if (!el.quantity.value) errors.quantity = "Informe a quantidade de peças.";
  else if (!Number.isInteger(qty) || qty < 1) errors.quantity = "Use um número inteiro maior que zero.";
  if (el.orderDate.value && el.orderDate.value > today) errors.orderDate = "A data do pedido não pode estar no futuro.";
  if (el.dueDate.value && el.orderDate.value && el.dueDate.value < el.orderDate.value) errors.dueDate = "A entrega não pode ser antes da data do pedido.";
  else if (el.dueDate.value && el.dueDate.value < today) errors.dueDate = "A entrega prevista deve ser hoje ou uma data futura.";
  if (el.phone.value.trim() && el.phone.value.replace(/\D/g, "").length < 10) errors.phone = "Telefone incompleto. Inclua o DDD.";

  ["client", "phone", "product", "quantity", "origin", "destination", "destinationRegion", "orderDate", "dueDate", "responsible"].forEach(name => setFieldError(el[name], errors[name]));
  const first = Object.keys(errors)[0];
  if (first) { el[first].focus(); showToast("Confira os campos destacados.", true); }
  return !first;
}

function orderPayload() {
  const f = new FormData(form);
  const text = name => String(f.get(name) || "").trim();
  return {
    client: text("client"), phone: text("phone"), product: text("product"), quantity: Number(f.get("quantity")),
    price: f.get("price") ? Number(f.get("price")) : null, orderDate: f.get("orderDate"), dueDate: f.get("dueDate"),
    stage: f.get("stage"), priority: f.get("priority"), notes: text("notes"), responsible: text("responsible"),
    origin: text("origin"), destination: text("destination"), destinationRegion: text("destinationRegion")
  };
}

// O backend não tem campos de prioridade e preço: eles são guardados nas observações para não se perderem.
function composeNotes(payload) {
  return [
    payload.priority === "Alta" ? "Prioridade: alta" : "",
    payload.price ? `Valor por peça: ${payload.price.toLocaleString("pt-BR", { style: "currency", currency: "BRL" })}` : "",
    payload.notes
  ].filter(Boolean).join("\n");
}

const brl = value => value.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
function updatePriceTotal() {
  const qty = Number(form.elements.quantity.value);
  const price = Number(form.elements.price.value);
  $("#price-total").textContent = qty > 0 && price > 0 ? `Total estimado: ${brl(qty * price)}` : "";
}

function resetFormDefaults() {
  form.elements.orderDate.value = todayISO();
  form.elements.orderDate.max = todayISO();
  form.elements.dueDate.min = todayISO();
  regionTouched = false;
  $$("#order-form .field.invalid").forEach(field => field.classList.remove("invalid"));
  $$("#order-form [aria-invalid]").forEach(input => input.removeAttribute("aria-invalid"));
  updatePriceTotal();
}

form.addEventListener("input", event => {
  const input = event.target;
  if (input.closest(".field.invalid")) setFieldError(input, "");
  if (input.name === "destinationRegion") regionTouched = Boolean(input.value);
  if (input.name === "destination" && !regionTouched) form.elements.destinationRegion.value = input.value;
  if (input.name === "quantity" || input.name === "price") updatePriceTotal();
  if (input.name === "orderDate" && input.value) form.elements.dueDate.min = input.value > todayISO() ? input.value : todayISO();
});
form.addEventListener("reset", () => window.setTimeout(resetFormDefaults, 0));

form.addEventListener("submit", async event => {
  event.preventDefault();
  if (!validateOrderForm()) return;
  const payload = orderPayload();
  const submit = form.querySelector('[type="submit"]');
  const label = submit.querySelector(".button-label");
  submit.disabled = true;
  label.textContent = "Salvando…";
  try {
    let order;
    const notes = composeNotes(payload);
    if (USE_DEMO_DATA) {
      const nextNumber = Math.max(0, ...orders.map(o => Number(String(o.code).split("-").pop()) || 0)) + 1;
      order = normalizeOrder({
        id: Date.now(), codigo: `PED-${new Date().getFullYear()}-${String(nextNumber).padStart(4, "0")}`,
        cliente: { nome: payload.client, telefone: payload.phone }, responsavel: payload.responsible, origem: payload.origin, destino: payload.destination,
        regiaoDestino: payload.destinationRegion, itens: [{ descricao: payload.product, quantidade: payload.quantity, unidade: "peças" }],
        dataPedido: payload.orderDate, previsaoEntrega: payload.dueDate, status: payload.stage, observacoes: notes, ultimaLocalizacao: payload.origin
      });
      const now = new Date().toISOString();
      historyEntries.push(normalizeHistory({ statusNovo: "PENDENTE", tipo: "CADASTRO", dataHora: now, responsavel: payload.responsible, localizacao: payload.origin }, order));
      if (payload.stage !== "PENDENTE") historyEntries.push(normalizeHistory({ statusNovo: payload.stage, tipo: "ALTERACAO_STATUS", dataHora: new Date(Date.now() + 1000).toISOString(), responsavel: payload.responsible, localizacao: payload.origin, descricao: `Pedido registrado já na etapa “${STATUS[payload.stage].label}”.` }, order));
    } else {
      const found = await orderApi.clients(payload.client);
      const match = (Array.isArray(found) ? found : found?.content || []).find(c => c.nome.trim().toLocaleLowerCase("pt-BR") === payload.client.toLocaleLowerCase("pt-BR"));
      const client = match || await orderApi.createClient({ nome: payload.client, telefone: payload.phone, cidade: payload.destination });
      const created = await orderApi.create({
        codigo: `PED-${new Date().getFullYear()}-${Date.now().toString().slice(-6)}`, clienteId: client.id, responsavel: payload.responsible,
        origem: payload.origin, destino: payload.destination, regiaoOrigem: "Agreste", regiaoDestino: payload.destinationRegion,
        itens: [{ descricao: payload.product, quantidade: payload.quantity, unidade: "peças" }], dataPedido: payload.orderDate,
        previsaoEntrega: payload.dueDate, observacoes: notes, ultimaLocalizacao: payload.origin
      });
      let latest = created;
      // A API só aceita avançar uma etapa por vez (pular exige perfil ADMIN), então percorremos o fluxo.
      try {
        for (const step of FLOW.slice(1, FLOW.indexOf(payload.stage) + 1)) {
          latest = await orderApi.updateStatus(created.id, step, ADVANCE_DESCRIPTION[step], payload.origin);
        }
      } catch (error) {
        showToast(`Pedido salvo, mas a etapa não pôde ser atualizada: ${error.message}`, true);
      }
      order = normalizeOrder(latest);
    }
    orders.unshift(order);
    if (!USE_DEMO_DATA) loadHistory(); // depois do unshift, para incluir o pedido novo
    form.reset();
    renderAll();
    goTo("consulta");
    showToast(USE_DEMO_DATA ? `${order.code} adicionado (modo demonstração).` : `${order.code} cadastrado com sucesso.`);
  } catch (error) {
    showToast(error.message || "Não foi possível salvar o pedido. Tente novamente.", true);
  } finally {
    submit.disabled = false;
    label.textContent = "Salvar pedido";
  }
});

/* ---------------- carregamento via API ---------------- */
async function loadOrders() {
  try {
    const response = await orderApi.list();
    orders = (Array.isArray(response) ? response : response?.content || []).map(normalizeOrder);
    dataState = "live";
  } catch (error) {
    dataState = "error";
    showToast(error.message || "Não foi possível carregar os pedidos.", true);
  }
  renderAll();
}

async function loadHistory() {
  const subset = orders.slice(0, HISTORY_ORDER_LIMIT);
  const results = await Promise.allSettled(subset.map(order => orderHistory(order)));
  historyEntries = results.flatMap(r => (r.status === "fulfilled" ? r.value : []));
  renderTimeline();
}

/* ---------------- eventos gerais ---------------- */
document.addEventListener("click", event => {
  const advance = event.target.closest("[data-advance]");
  if (advance) { advanceOrder(advance.dataset.advance, advance.dataset.next, advance); return; }
  const statusLink = event.target.closest("[data-filter-status]");
  if (statusLink) {
    $("#status-filter").value = statusLink.dataset.filterStatus;
    $("#order-search").value = "";
    renderConsultation();
    goTo("consulta");
    return;
  }
  const orderEl = event.target.closest("[data-order]");
  if (orderEl && !event.target.closest("a")) openDrawer(orderEl.dataset.order);
});
document.addEventListener("keydown", event => {
  if ((event.key === "Enter" || event.key === " ") && event.target.matches("tr[data-order]")) { event.preventDefault(); openDrawer(event.target.dataset.order); }
});
$("#order-search").addEventListener("input", renderConsultation);
$("#status-filter").addEventListener("change", renderConsultation);
$("#sort-order").addEventListener("change", renderConsultation);
$("#history-period").addEventListener("change", renderTimeline);
$("#clear-filters").addEventListener("click", () => {
  $("#order-search").value = "";
  $("#status-filter").value = "";
  $("#sort-order").value = "recent";
  renderConsultation();
  $("#order-search").focus();
});

/* ---------------- tema ---------------- */
const themeToggle = $("#theme-toggle");
function syncThemeButton() {
  const dark = document.documentElement.dataset.theme === "dark";
  themeToggle.setAttribute("aria-label", dark ? "Ativar modo claro" : "Ativar modo escuro");
}
themeToggle.addEventListener("click", () => {
  const next = document.documentElement.dataset.theme === "dark" ? "light" : "dark";
  document.documentElement.dataset.theme = next;
  try { localStorage.setItem("fio-a-fio-theme", next); } catch { /* segue sem salvar */ }
  syncThemeButton();
  renderDashboard(); // o donut usa as cores do tema
});

/* ---------------- início ---------------- */
const now = new Date();
$("#today-date").textContent = fmt({ weekday: "short", day: "2-digit", month: "short" }, now);
$("#dashboard-date").textContent = fmt({ weekday: "long", day: "2-digit", month: "long" }, now);
syncThemeButton();
resetFormDefaults();
showView(currentView(), { focus: false });

if (USE_DEMO_DATA) {
  const demo = buildDemoData();
  orders = demo.demoOrders;
  historyEntries = demo.demoHistory;
  renderAll();
} else {
  renderAll();
  loadOrders().then(() => { if (dataState === "live") loadHistory(); });
}
