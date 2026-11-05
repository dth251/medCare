/**
 * MEDCARE – BỘ ICON SVG NÉT MẢNH (đường nét theo Lucide, giấy phép ISC)
 * Dùng: <i data-icon="arrow-left"></i>  hoặc trong JS: ic('arrow-left')
 * Mọi phần tử i[data-icon] (kể cả HTML do JS dựng sau này) được tự động vẽ thành SVG.
 */
const ICON_PATHS = {
  'arrow-left': '<path d="m12 19-7-7 7-7"/><path d="M19 12H5"/>',
  'arrow-right': '<path d="M5 12h14"/><path d="m12 5 7 7-7 7"/>',
  'chevron-right': '<path d="m9 18 6-6-6-6"/>',
  'x': '<path d="M18 6 6 18"/><path d="m6 6 12 12"/>',
  'check': '<path d="M20 6 9 17l-5-5"/>',
  'circle-check': '<circle cx="12" cy="12" r="10"/><path d="m9 12 2 2 4-4"/>',
  'alert': '<circle cx="12" cy="12" r="10"/><path d="M12 8v4"/><path d="M12 16h.01"/>',
  'log-out': '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="m16 17 5-5-5-5"/><path d="M21 12H9"/>',
  'eye': '<path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/>',
  'eye-off': '<path d="M9.88 9.88a3 3 0 1 0 4.24 4.24"/><path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68"/><path d="M6.61 6.61A13.53 13.53 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61"/><path d="M2 2l20 20"/>',
  'printer': '<path d="M6 9V2h12v7"/><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><rect width="12" height="8" x="6" y="14"/>',
  'trash': '<path d="M3 6h18"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>',
  'search': '<circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>',
  'zap': '<path d="M13 2 3 14h9l-1 8 10-12h-9l1-8Z"/>',
  'lock': '<rect width="18" height="11" x="3" y="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>',
  'loader': '<path d="M21 12a9 9 0 1 1-6.22-8.56"/>',
  'pencil': '<path d="M17 3a2.85 2.85 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/>',
  'cross': '<path d="M12 5v14"/><path d="M5 12h14"/>'
};

/** Chuỗi HTML của một icon (được vẽ thành SVG tự động). */
function ic(name, cls = '') {
  return `<i data-icon="${name}"${cls ? ` class="${cls}"` : ''}></i>`;
}

function renderIcon(el) {
  const name = el.getAttribute('data-icon');
  const paths = ICON_PATHS[name];
  if (!paths || el.dataset.iconDrawn === name) return;
  const sw = el.getAttribute('data-stroke') || '2';
  el.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="${sw}" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths}</svg>`;
  el.dataset.iconDrawn = name;
}

function hydrateIcons(root) {
  if (!root) return;
  if (root.nodeType === 1 && root.matches('i[data-icon]')) renderIcon(root);
  if (root.querySelectorAll) root.querySelectorAll('i[data-icon]').forEach(renderIcon);
}

(function startIconObserver() {
  const start = () => {
    hydrateIcons(document.body);
    new MutationObserver(mutations => {
      for (const m of mutations) {
        if (m.type === 'attributes') renderIcon(m.target);
        else m.addedNodes.forEach(n => { if (n.nodeType === 1) hydrateIcons(n); });
      }
    }).observe(document.body, { childList: true, subtree: true, attributes: true, attributeFilter: ['data-icon'] });
  };
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start);
  else start();
})();
