function area(kind) {
  return kind === 'session' ? window.sessionStorage : window.localStorage;
}

export function load(kind, key) {
  try {
    return area(kind).getItem(key);
  } catch {
    return null;
  }
}

export function save(kind, key, value) {
  try {
    area(kind).setItem(key, value);
  } catch {
    return;
  }
}
