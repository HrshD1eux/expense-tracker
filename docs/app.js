/* ==========================================================================
   Expense Tracker — Interactive Web Logic
   ========================================================================== */

document.addEventListener('DOMContentLoaded', () => {
  initThemeToggle();
  fetchLatestRelease();
  initInteractiveCalculator();
  initCopyChecksum();
});

/* --------------------------------------------------------------------------
   1. Light / Dark Theme Switcher (Default: Light Theme)
   -------------------------------------------------------------------------- */
function initThemeToggle() {
  const themeToggleBtn = document.getElementById('theme-toggle');
  const savedTheme = localStorage.getItem('theme') || 'light';
  
  document.documentElement.setAttribute('data-theme', savedTheme);

  if (themeToggleBtn) {
    themeToggleBtn.addEventListener('click', () => {
      const currentTheme = document.documentElement.getAttribute('data-theme');
      const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
      
      document.documentElement.setAttribute('data-theme', newTheme);
      localStorage.setItem('theme', newTheme);
    });
  }
}

/* --------------------------------------------------------------------------
   2. Dynamic GitHub Releases API Fetcher
   -------------------------------------------------------------------------- */
async function fetchLatestRelease() {
  const repo = 'HrshD1eux/expense-tracker';
  const apiUrl = `https://api.github.com/repos/${repo}/releases/latest`;

  const heroBtn = document.getElementById('hero-download-btn');
  const heroLabel = document.getElementById('hero-release-label');
  const directBtn = document.getElementById('direct-apk-btn');
  const directVersion = document.getElementById('direct-apk-version');
  const checksumText = document.getElementById('checksum-text');

  try {
    const res = await fetch(apiUrl);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();

    const tag = data.tag_name || 'v1.0.0';
    const apkAsset = data.assets?.find(a => a.name.endsWith('.apk'));

    if (apkAsset) {
      const downloadUrl = apkAsset.browser_download_url;
      const sizeMB = (apkAsset.size / (1024 * 1024)).toFixed(1);
      const label = `${tag} • ${sizeMB} MB • ${apkAsset.download_count || 0} Downloads`;

      if (heroBtn) heroBtn.href = downloadUrl;
      if (heroLabel) heroLabel.textContent = label;
      if (directBtn) directBtn.href = downloadUrl;
      if (directVersion) directVersion.textContent = `${tag} • ${sizeMB} MB • Signed Release`;
    }

    // Try parsing SHA-256 from release notes body
    if (data.body && checksumText) {
      const match = data.body.match(/SHA-256.*?`([a-f0-9]{64})`/i) ||
                    data.body.match(/`SHA256:\s*([a-f0-9]{64})`/i);
      if (match && match[1]) {
        checksumText.textContent = match[1];
      }
    }
  } catch (err) {
    console.log('GitHub API offline or rate-limited. Falling back to default release URLs.', err);
  }
}

/* --------------------------------------------------------------------------
   3. Interactive In-line Calculator Demo
   -------------------------------------------------------------------------- */
function initInteractiveCalculator() {
  const formulaEl = document.getElementById('calc-formula');
  const resultEl = document.getElementById('calc-result');
  const buttons = document.querySelectorAll('.calc-btn');

  let currentExpr = '1450 / 3 + 45';

  function evaluate(expr) {
    if (!expr || expr.trim() === '') return '₹ 0.00';
    try {
      // Clean and sanitize string: allow only digits and basic operators
      const sanitized = expr.replace(/×/g, '*').replace(/÷/g, '/');
      if (!/^[\d\s+\-*/.]+$/.test(sanitized)) return 'Error';

      // Evaluate safely
      const fn = new Function(`return (${sanitized})`);
      const val = fn();

      if (typeof val === 'number' && !isNaN(val) && isFinite(val)) {
        return '₹ ' + val.toLocaleString('en-IN', {
          minimumFractionDigits: 2,
          maximumFractionDigits: 2
        });
      }
      return 'Error';
    } catch {
      return '...';
    }
  }

  function update() {
    formulaEl.textContent = currentExpr || '0';
    resultEl.textContent = evaluate(currentExpr);
  }

  buttons.forEach(btn => {
    btn.addEventListener('click', () => {
      const val = btn.getAttribute('data-val');

      if (val === 'C') {
        currentExpr = '';
      } else if (val === 'DEL') {
        currentExpr = currentExpr.slice(0, -1);
      } else if (val === '=') {
        const res = evaluate(currentExpr);
        if (res !== 'Error' && res !== '...') {
          currentExpr = res.replace('₹ ', '').replace(/,/g, '');
        }
      } else if (['+', '-', '*', '/'].includes(val)) {
        currentExpr += ` ${val} `;
      } else {
        currentExpr += val;
      }

      update();
    });
  });

  update();
}

/* --------------------------------------------------------------------------
   4. Copy Checksum to Clipboard
   -------------------------------------------------------------------------- */
function initCopyChecksum() {
  const copyBtn = document.getElementById('copy-checksum');
  const checksumText = document.getElementById('checksum-text');

  if (copyBtn && checksumText) {
    copyBtn.addEventListener('click', () => {
      const text = checksumText.textContent.trim();
      navigator.clipboard.writeText(text).then(() => {
        const originalText = copyBtn.textContent;
        copyBtn.textContent = 'Copied!';
        copyBtn.style.color = '#10B981';
        setTimeout(() => {
          copyBtn.textContent = originalText;
          copyBtn.style.color = '';
        }, 2000);
      });
    });
  }
}
