const TOAST_TIME = 3000;

const status = document.getElementById('status');
const toast = document.getElementById('toast');
const gone = document.getElementById('gone');
const goneText = document.getElementById('gone-text');

let toastTimer = null;

export function showStatus(state, text) {
  status.dataset.state = state;
  status.textContent = text;
}

export function showToast(text) {
  toast.textContent = text;
  toast.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    toast.hidden = true;
  }, TOAST_TIME);
}

export function showGone(text) {
  showStatus('offline', 'Not connected');
  goneText.textContent = text;
  gone.hidden = false;
}
