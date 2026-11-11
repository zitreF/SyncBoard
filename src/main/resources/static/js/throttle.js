export function throttled(action, interval) {
  let timer = null;
  return () => {
    if (timer !== null) {
      return;
    }
    timer = setTimeout(() => {
      timer = null;
      action();
    }, interval);
  };
}
