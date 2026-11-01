# BLS Spain Visa — Personal Form Autofill

A small Tampermonkey userscript that fills the BLS visa **application form** from
your own saved profile, so you don't retype your passport number and address by
hand every time. You stay in control: **you** log in, clear the captcha, reach
the form, then click **Fill**, review, and submit yourself.

## What it does / doesn't do

**Does:** type your saved personal data into the visible form fields, then stop.

**Doesn't:** log in, solve captchas, read OTPs, poll for slots, or press Submit.
Those cross into the auto-booking/scalping territory that gets accounts banned and
takes appointments from people applying by hand — deliberately left out.

Your data lives **only in this browser** (localStorage). It is never written to a
file, never synced, never committed to this repo.

## Install

1. Install the **Tampermonkey** extension (Chrome/Firefox/Edge).
2. Tampermonkey → *Dashboard* → *+* (Create a new script) → paste the contents of
   `bls-autofill.user.js` → Save. (Or drag the `.user.js` file onto the dashboard.)
3. Open the BLS site. A small **BLS Autofill** panel appears bottom-right.

## Set up your profile

1. In the panel, expand **Profile**.
2. Click **Load example**, replace the values with your real details, click **Save profile**.
3. It's remembered on this machine until you click **Forget**.

## Tune it to the real form (one-time)

The script matches fields by the form's **labels / names / placeholders**. BLS
labels may differ from the defaults. If some fields are reported "Not matched"
after a fill:

1. Right-click the field on the page → *Inspect*.
2. Note its label text, `name`, `id`, or `placeholder`.
3. Add that word to the matching `hints` for the right key in the `FIELD_MAP`
   section of `bls-autofill.user.js`.

## Notes

- `@match` is set for `*.blsspainvisa.com` and `*.blsspainmorocco.net`. Adjust if
  your consulate uses a different host.
- The sensitive fields (passport, etc.) stay in your browser. If you share the
  computer, click **Forget** when done.
- This is unrelated to the aniTV project it currently sits in; move it anywhere.
