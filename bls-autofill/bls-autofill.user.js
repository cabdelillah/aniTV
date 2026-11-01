// ==UserScript==
// @name         BLS Spain Visa — Personal Form Autofill (review & submit yourself)
// @namespace    personal.bls.autofill
// @version      1.0.0
// @description  Fills the BLS visa application form from YOUR own saved profile, then stops so you can review and submit by hand. No login, no captcha, no OTP, no auto-submit.
// @match        https://*.blsspainvisa.com/*
// @match        https://*.blsspainmorocco.net/*
// @run-at       document-idle
// @grant        none
// ==/UserScript==

/*
 * WHAT THIS IS / IS NOT
 * ---------------------
 * This is a convenience autofill, like a password manager. You log in, clear the
 * captcha, and navigate to the application form YOURSELF. Then you click "Fill",
 * the script types your saved details into the visible fields, and it STOPS.
 * You read everything over and click the real Submit button yourself.
 *
 * It deliberately does NOT: log in, solve captchas, read OTPs, poll for slots,
 * or press submit. Those are the parts that break other applicants' access and
 * get accounts banned — they're not here on purpose.
 *
 * YOUR DATA stays in this browser only (localStorage), never in a file or repo.
 * Open the panel, paste your profile JSON once, and it's remembered on this
 * machine. Use "Forget" to wipe it.
 */

(function () {
  'use strict';

  // ----------------------------------------------------------------------------
  // 1) PROFILE SCHEMA  (example — edit via the panel, not here)
  //    These are common Schengen application fields. Keep only what your form uses.
  // ----------------------------------------------------------------------------
  const EXAMPLE_PROFILE = {
    surname: 'YOUR SURNAME',
    firstName: 'Your First Name',
    surnameAtBirth: '',
    dateOfBirth: '1990-01-31',     // YYYY-MM-DD
    placeOfBirth: 'Casablanca',
    countryOfBirth: 'Morocco',
    currentNationality: 'Moroccan',
    sex: 'Male',                   // Male | Female
    maritalStatus: 'Single',
    passportNumber: 'AB1234567',
    passportIssueDate: '2022-05-01',
    passportExpiryDate: '2032-04-30',
    passportIssuedBy: 'Morocco',
    nationalId: '',
    email: 'you@example.com',
    phone: '+212600000000',
    addressLine: '123 Rue Example',
    city: 'Rabat',
    postalCode: '10000',
    country: 'Morocco',
    occupation: 'Engineer',
    employer: 'Example Co.',
    purposeOfTravel: 'Tourism',
    destinationState: 'Spain',
    firstEntryState: 'Spain',
    numberOfEntries: 'Multiple',
    arrivalDate: '2026-07-01',
    departureDate: '2026-07-15',
  };

  // ----------------------------------------------------------------------------
  // 2) FIELD MAP  (profile key  ->  how to find the field on the page)
  //    Each entry lists "hints": substrings matched (case-insensitive) against a
  //    field's label text, name, id, placeholder, or aria-label. First match wins.
  //    EDIT THESE to match the real BLS form labels — that's the one tuning step.
  // ----------------------------------------------------------------------------
  const FIELD_MAP = [
    { key: 'surname',            hints: ['surname', 'last name', 'nom'] },
    { key: 'firstName',          hints: ['first name', 'given name', 'prénom', 'prenom'] },
    { key: 'surnameAtBirth',     hints: ['surname at birth', 'nom de naissance'] },
    { key: 'dateOfBirth',        hints: ['date of birth', 'birth date', 'date de naissance', 'dob'] },
    { key: 'placeOfBirth',       hints: ['place of birth', 'lieu de naissance'] },
    { key: 'countryOfBirth',     hints: ['country of birth', 'pays de naissance'] },
    { key: 'currentNationality', hints: ['nationality', 'nationalité', 'nationalite'] },
    { key: 'sex',                hints: ['sex', 'gender', 'sexe'] },
    { key: 'maritalStatus',      hints: ['marital', 'état civil', 'etat civil'] },
    { key: 'passportNumber',     hints: ['passport number', 'document number', 'n° passeport', 'numero passeport'] },
    { key: 'passportIssueDate',  hints: ['date of issue', 'issue date', 'date de délivrance', 'date de delivrance'] },
    { key: 'passportExpiryDate', hints: ['date of expiry', 'expiry', 'valid until', 'expiration'] },
    { key: 'passportIssuedBy',   hints: ['issued by', 'issuing', 'délivré par', 'delivre par', 'issuing authority'] },
    { key: 'nationalId',         hints: ['national id', 'cin', 'id number'] },
    { key: 'email',              hints: ['email', 'e-mail', 'courriel'] },
    { key: 'phone',              hints: ['phone', 'mobile', 'telephone', 'téléphone', 'tel'] },
    { key: 'addressLine',        hints: ['address', 'adresse', 'street'] },
    { key: 'city',               hints: ['city', 'ville', 'town'] },
    { key: 'postalCode',         hints: ['postal', 'zip', 'code postal'] },
    { key: 'country',            hints: ['country of residence', 'country', 'pays'] },
    { key: 'occupation',         hints: ['occupation', 'profession', 'job'] },
    { key: 'employer',           hints: ['employer', 'company', 'employeur'] },
    { key: 'purposeOfTravel',    hints: ['purpose', 'motif', 'reason for'] },
    { key: 'destinationState',   hints: ['destination', 'main destination', 'member state of destination'] },
    { key: 'firstEntryState',    hints: ['first entry', 'state of first entry'] },
    { key: 'numberOfEntries',    hints: ['number of entries', 'entries requested'] },
    { key: 'arrivalDate',        hints: ['intended date of arrival', 'arrival', 'date of entry', 'arrivée', 'arrivee'] },
    { key: 'departureDate',      hints: ['intended date of departure', 'departure', 'départ', 'depart'] },
  ];

  const LS_KEY = 'bls_autofill_profile_v1';

  // ----------------------------------------------------------------------------
  // helpers
  // ----------------------------------------------------------------------------
  const norm = (s) => (s || '').toString().toLowerCase().replace(/\s+/g, ' ').trim();

  function loadProfile() {
    try {
      const raw = localStorage.getItem(LS_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch (e) {
      return null;
    }
  }
  function saveProfile(obj) {
    localStorage.setItem(LS_KEY, JSON.stringify(obj));
  }

  // Find the human-readable label text associated with an input element.
  function labelTextFor(el) {
    let text = '';
    if (el.id) {
      const lab = document.querySelector(`label[for="${CSS.escape(el.id)}"]`);
      if (lab) text += ' ' + lab.textContent;
    }
    const wrapLabel = el.closest('label');
    if (wrapLabel) text += ' ' + wrapLabel.textContent;
    // look a little way up the DOM for a nearby label/legend/text
    let p = el.parentElement, hops = 0;
    while (p && hops < 3) {
      const lab = p.querySelector('label, legend, .control-label, .form-label');
      if (lab) { text += ' ' + lab.textContent; break; }
      p = p.parentElement; hops++;
    }
    return norm(text);
  }

  // Build a searchable signature for a field from all its identifying attributes.
  function fieldSignature(el) {
    return norm([
      el.name, el.id, el.placeholder,
      el.getAttribute('aria-label'),
      el.getAttribute('data-testid'),
      labelTextFor(el),
    ].filter(Boolean).join(' '));
  }

  // Set a value in a way React/Angular/vanilla all notice.
  function setNativeValue(el, value) {
    const proto = el.tagName === 'TEXTAREA'
      ? window.HTMLTextAreaElement.prototype
      : window.HTMLInputElement.prototype;
    const setter = Object.getOwnPropertyDescriptor(proto, 'value')?.set;
    if (setter) setter.call(el, value); else el.value = value;
    el.dispatchEvent(new Event('input', { bubbles: true }));
    el.dispatchEvent(new Event('change', { bubbles: true }));
    el.dispatchEvent(new Event('blur', { bubbles: true }));
  }

  function fillText(el, value) {
    setNativeValue(el, value);
    flash(el);
  }

  function fillSelect(el, value) {
    const want = norm(value);
    let matched = null;
    for (const opt of el.options) {
      const t = norm(opt.textContent), v = norm(opt.value);
      if (t === want || v === want) { matched = opt; break; }
    }
    if (!matched) {
      for (const opt of el.options) {
        if (norm(opt.textContent).includes(want) && want) { matched = opt; break; }
      }
    }
    if (matched) {
      el.value = matched.value;
      el.dispatchEvent(new Event('change', { bubbles: true }));
      flash(el);
      return true;
    }
    return false;
  }

  function fillRadio(groupEls, value) {
    const want = norm(value);
    for (const el of groupEls) {
      const sig = fieldSignature(el) + ' ' + norm(el.value);
      if (sig.includes(want) && want) {
        el.checked = true;
        el.dispatchEvent(new Event('change', { bubbles: true }));
        flash(el);
        return true;
      }
    }
    return false;
  }

  function flash(el) {
    const prev = el.style.outline;
    el.style.outline = '2px solid #16a34a';
    setTimeout(() => { el.style.outline = prev; }, 1500);
  }

  // ----------------------------------------------------------------------------
  // the fill pass
  // ----------------------------------------------------------------------------
  function runFill() {
    const profile = loadProfile();
    if (!profile) {
      setStatus('No profile saved. Paste your JSON and click "Save profile" first.', true);
      return;
    }

    const inputs = Array.from(document.querySelectorAll('input, select, textarea'))
      .filter((el) => el.type !== 'hidden' && el.type !== 'password' && !el.disabled && el.offsetParent !== null);

    const radioGroups = {};
    for (const el of inputs) {
      if (el.type === 'radio') {
        (radioGroups[el.name] = radioGroups[el.name] || []).push(el);
      }
    }

    const used = new Set();
    let filled = 0, skipped = [];

    for (const { key, hints } of FIELD_MAP) {
      const value = profile[key];
      if (value === undefined || value === null || value === '') continue;

      // radios first (by group)
      let done = false;
      for (const name in radioGroups) {
        const group = radioGroups[name];
        const groupSig = group.map(fieldSignature).join(' ');
        if (hints.some((h) => groupSig.includes(norm(h)))) {
          if (fillRadio(group, value)) { filled++; done = true; break; }
        }
      }
      if (done) continue;

      // text / select / textarea
      const candidate = inputs.find((el) => {
        if (used.has(el) || el.type === 'radio' || el.type === 'checkbox') return false;
        const sig = fieldSignature(el);
        return hints.some((h) => sig.includes(norm(h)));
      });

      if (!candidate) { skipped.push(key); continue; }
      used.add(candidate);

      if (candidate.tagName === 'SELECT') {
        if (fillSelect(candidate, value)) filled++; else skipped.push(key + ' (no option)');
      } else {
        fillText(candidate, String(value));
        filled++;
      }
    }

    // jump to the first empty required field so you know where to look
    const firstEmptyRequired = inputs.find((el) =>
      (el.required || el.getAttribute('aria-required') === 'true') && !el.value);
    if (firstEmptyRequired) firstEmptyRequired.scrollIntoView({ behavior: 'smooth', block: 'center' });

    setStatus(
      `Filled ${filled} field(s). ${skipped.length ? 'Not matched: ' + skipped.join(', ') + '. ' : ''}` +
      `Review everything, then submit yourself.`,
      skipped.length > 0
    );
  }

  // ----------------------------------------------------------------------------
  // the floating panel UI
  // ----------------------------------------------------------------------------
  let statusEl;
  function setStatus(msg, warn) {
    if (!statusEl) return;
    statusEl.textContent = msg;
    statusEl.style.color = warn ? '#b45309' : '#166534';
  }

  function buildPanel() {
    if (document.getElementById('bls-autofill-panel')) return;

    const panel = document.createElement('div');
    panel.id = 'bls-autofill-panel';
    panel.style.cssText = [
      'position:fixed', 'bottom:16px', 'right:16px', 'z-index:2147483647',
      'width:320px', 'font:13px/1.4 system-ui,Arial,sans-serif',
      'background:#fff', 'border:1px solid #d1d5db', 'border-radius:10px',
      'box-shadow:0 8px 24px rgba(0,0,0,.18)', 'padding:12px', 'color:#111',
    ].join(';');

    panel.innerHTML = `
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
        <strong>BLS Autofill</strong>
        <button id="bls-min" title="minimize" style="border:none;background:#f3f4f6;border-radius:6px;cursor:pointer;padding:2px 8px">–</button>
      </div>
      <div id="bls-body">
        <div style="font-size:11px;color:#6b7280;margin-bottom:6px">
          Fills YOUR saved data into the form. You review &amp; submit. No login/captcha/submit.
        </div>
        <button id="bls-fill" style="width:100%;padding:9px;border:none;border-radius:8px;background:#16a34a;color:#fff;font-weight:600;cursor:pointer;margin-bottom:8px">Fill form</button>
        <details style="margin-bottom:8px">
          <summary style="cursor:pointer;color:#374151">Profile (stored only in this browser)</summary>
          <textarea id="bls-json" spellcheck="false" style="width:100%;height:150px;margin-top:6px;font:11px monospace;border:1px solid #d1d5db;border-radius:6px;padding:6px;box-sizing:border-box"></textarea>
          <div style="display:flex;gap:6px;margin-top:6px">
            <button id="bls-save" style="flex:1;padding:7px;border:1px solid #d1d5db;border-radius:6px;background:#f9fafb;cursor:pointer">Save profile</button>
            <button id="bls-example" style="flex:1;padding:7px;border:1px solid #d1d5db;border-radius:6px;background:#f9fafb;cursor:pointer">Load example</button>
            <button id="bls-forget" style="flex:1;padding:7px;border:1px solid #fecaca;border-radius:6px;background:#fef2f2;color:#b91c1c;cursor:pointer">Forget</button>
          </div>
        </details>
        <div id="bls-status" style="font-size:11px;min-height:16px"></div>
      </div>
    `;
    document.body.appendChild(panel);

    statusEl = panel.querySelector('#bls-status');
    const ta = panel.querySelector('#bls-json');
    const saved = loadProfile();
    ta.value = JSON.stringify(saved || EXAMPLE_PROFILE, null, 2);
    if (saved) setStatus('Profile loaded from this browser.');

    panel.querySelector('#bls-fill').onclick = runFill;
    panel.querySelector('#bls-save').onclick = () => {
      try {
        const obj = JSON.parse(ta.value);
        saveProfile(obj);
        setStatus('Saved in this browser.');
      } catch (e) {
        setStatus('That is not valid JSON.', true);
      }
    };
    panel.querySelector('#bls-example').onclick = () => {
      ta.value = JSON.stringify(EXAMPLE_PROFILE, null, 2);
      setStatus('Example loaded — edit it, then Save.');
    };
    panel.querySelector('#bls-forget').onclick = () => {
      localStorage.removeItem(LS_KEY);
      ta.value = JSON.stringify(EXAMPLE_PROFILE, null, 2);
      setStatus('Profile wiped from this browser.');
    };
    const body = panel.querySelector('#bls-body');
    panel.querySelector('#bls-min').onclick = () => {
      body.style.display = body.style.display === 'none' ? 'block' : 'none';
    };
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', buildPanel);
  } else {
    buildPanel();
  }
})();
