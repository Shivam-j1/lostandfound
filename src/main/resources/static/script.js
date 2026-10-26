const form = document.getElementById('item-form');
const typeSelect = document.getElementById('item-type');
const securityFields = document.getElementById('security-fields');
const errorSummary = document.getElementById('error-summary');
const formSuccess = document.getElementById('form-success');
const itemsGrid = document.getElementById('items-grid');
const searchInput = document.getElementById('search-input');
const filterType = document.getElementById('filter-type');

// field id -> { input id, label } used by the client-side validator
const FIELDS = {
  title: { id: 'item-title', label: 'Item title' },
  location: { id: 'item-location', label: 'Campus location' },
  contactInfo: { id: 'contact-info', label: 'Email or phone' },
  securityQuestion: { id: 'security-question', label: 'Verification question' },
  securityAnswer: { id: 'security-answer', label: 'Expected answer' }
};

typeSelect.addEventListener('change', () => {
  securityFields.style.display = typeSelect.value === 'FOUND' ? 'block' : 'none';
});

/* ============================================================
   FRONTEND FEATURE 1: Live search + filter + stats dashboard
   ============================================================ */
let debounceTimer;
function debouncedLoad() {
  clearTimeout(debounceTimer);
  debounceTimer = setTimeout(loadItems, 250);
}

async function loadItems() {
  const search = searchInput.value.trim();
  const type = filterType.value;
  let url = '/api/items';
  const params = new URLSearchParams();
  if (search) params.set('search', search);
  if (type !== 'ALL') params.set('type', type);
  const query = params.toString();
  if (query) url += `?${query}`;
  try {
    const res = await fetch(url);
    if (!res.ok) throw new Error('Server error');
    renderItems(await res.json());
  } catch (e) {
    itemsGrid.innerHTML = '<p class="error-text">Could not load items. Is the server running?</p>';
  }
  loadStats();
}

async function loadStats() {
  try {
    const res = await fetch('/api/items/stats');
    const s = await res.json();
    document.getElementById('stat-total').textContent = s.total;
    document.getElementById('stat-lost').textContent = s.lost;
    document.getElementById('stat-found').textContent = s.found;
    document.getElementById('stat-claimed').textContent = s.claimed;
  } catch (e) { /* stats are optional */ }
}

function renderItems(items) {
  if (items.length === 0) {
    itemsGrid.innerHTML = '<p>No matching items found.</p>';
    return;
  }

  itemsGrid.innerHTML = items.map(item => {
    const isClaimed = item.status === 'CLAIMED';
    const isLocked = item.status === 'LOCKED';
    let badgeClass, badgeLabel;
    if (isClaimed) { badgeClass = 'badge-claimed'; badgeLabel = 'Claimed & Resolved'; }
    else if (isLocked) { badgeClass = 'badge-locked'; badgeLabel = 'Locked - See Security Desk'; }
    else if (item.type === 'FOUND') { badgeClass = 'badge-found'; badgeLabel = 'Found Item'; }
    else { badgeClass = 'badge-lost'; badgeLabel = 'Lost Item'; }

    const canClaim = item.type === 'FOUND' && item.status === 'OPEN';

    return `
      <article class="item-card">
        <span class="status-badge ${badgeClass}">
          <span aria-hidden="true">●</span> ${badgeLabel}
        </span>
        <h3>${escapeHtml(item.title)}</h3>
        <img
          src="https://placehold.co/400x220/1e3a8a/ffffff?text=${encodeURIComponent(item.category)}"
          alt="${escapeHtml(item.category)} item reported at ${escapeHtml(item.location)}"
          class="item-image"
        >
        <p><strong>Category:</strong> ${escapeHtml(item.category)}</p>
        <p><strong>Location:</strong> ${escapeHtml(item.location)}</p>
        <p><strong>Contact:</strong> ${escapeHtml(item.contactInfo)}</p>

        ${canClaim ? `
          <div class="claim-box">
            <label for="ans-${item.id}">Security question: ${escapeHtml(item.securityQuestion || 'Describe an item detail')}</label>
            <input type="text" id="ans-${item.id}" placeholder="Enter your answer to claim">
            <button type="button" class="btn-secondary" onclick="claimItem(${item.id})">
              Verify & Claim Item
            </button>
            <p id="claim-msg-${item.id}" role="alert" hidden></p>
          </div>
        ` : ''}
      </article>
    `;
  }).join('');
}

/* ============================================================
   FRONTEND FEATURE 2: Accessible client-side form validation
   (inline errors, aria-invalid, error summary, focus handling)
   ============================================================ */
function clearErrors() {
  errorSummary.hidden = true;
  errorSummary.innerHTML = '';
  Object.values(FIELDS).forEach(f => {
    const input = document.getElementById(f.id);
    const err = document.getElementById(f.id + '-error');
    input.removeAttribute('aria-invalid');
    err.hidden = true;
    err.textContent = '';
  });
}

function showFieldError(key, message) {
  const f = FIELDS[key];
  if (!f) return;
  const input = document.getElementById(f.id);
  const err = document.getElementById(f.id + '-error');
  input.setAttribute('aria-invalid', 'true');
  err.textContent = '⚠️ ' + message;
  err.hidden = false;
}

function validateForm() {
  const errors = {};
  const isFound = typeSelect.value === 'FOUND';
  const v = key => document.getElementById(FIELDS[key].id).value.trim();

  if (v('title').length < 3) errors.title = 'Enter an item title of at least 3 characters.';
  if (!v('location')) errors.location = 'Enter where the item was lost or found.';

  const contact = v('contactInfo');
  const emailOk = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(contact);
  const phoneOk = /^[+]?[0-9\s-]{7,15}$/.test(contact);
  if (!contact) errors.contactInfo = 'Enter your email or phone number.';
  else if (!emailOk && !phoneOk) errors.contactInfo = 'Enter a valid email (name@campus.edu) or phone number.';

  if (isFound) {
    if (!v('securityQuestion')) errors.securityQuestion = 'Enter a question only the real owner can answer.';
    if (!v('securityAnswer')) errors.securityAnswer = 'Enter the expected answer.';
  }
  return errors;
}

function showErrors(errors) {
  const keys = Object.keys(errors);
  keys.forEach(k => showFieldError(k, errors[k]));
  errorSummary.innerHTML =
    `<strong>Please fix ${keys.length} error${keys.length > 1 ? 's' : ''} below:</strong><ul>` +
    keys.map(k => `<li><a href="#${FIELDS[k] ? FIELDS[k].id : ''}">${escapeHtml(errors[k])}</a></li>`).join('') +
    '</ul>';
  errorSummary.hidden = false;
  errorSummary.focus();
}

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  clearErrors();
  formSuccess.hidden = true;

  const errors = validateForm();
  if (Object.keys(errors).length > 0) {
    showErrors(errors);
    return;
  }

  const payload = {
    type: typeSelect.value,
    title: document.getElementById('item-title').value.trim(),
    category: document.getElementById('item-category').value,
    location: document.getElementById('item-location').value.trim(),
    contactInfo: document.getElementById('contact-info').value.trim(),
    securityQuestion: document.getElementById('security-question').value.trim(),
    securityAnswer: document.getElementById('security-answer').value.trim()
  };

  try {
    const res = await fetch('/api/items', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      // Server-side validation errors come back as { errors: { field: message } }
      const data = await res.json();
      showErrors(data.errors || { title: 'Something went wrong. Please try again.' });
      return;
    }

    form.reset();
    securityFields.style.display = 'block';
    formSuccess.textContent = '✅ Report submitted and saved to the database!';
    formSuccess.hidden = false;
    loadItems();
  } catch (err) {
    showErrors({ title: 'Could not reach the server. Is it running?' });
  }
});

/* Claim an item: backend verifies the answer */
async function claimItem(id) {
  const input = document.getElementById(`ans-${id}`);
  const msgBox = document.getElementById(`claim-msg-${id}`);

  const res = await fetch(`/api/items/${id}/claim`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ answer: input.value })
  });
  const data = await res.json();

  msgBox.hidden = false;
  if (res.ok) {
    msgBox.className = 'success-text';
    msgBox.textContent = '✅ ' + data.message;
    setTimeout(loadItems, 3500);
  } else {
    msgBox.className = 'error-text';
    msgBox.textContent = '⚠️ ' + data.message;
    if (res.status === 423 || data.attemptsLeft === 0) setTimeout(loadItems, 2500);
  }
}

function escapeHtml(str) {
  return String(str == null ? '' : str).replace(/[&<>"']/g, m => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  }[m]));
}

searchInput.addEventListener('input', debouncedLoad);
filterType.addEventListener('change', loadItems);
loadItems();
