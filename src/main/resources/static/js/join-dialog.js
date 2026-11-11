import { load, save } from './storage.js';

const NAME_KEY = 'syncboard.name';

export class JoinDialog {
  #dialog;
  #nameInput;
  #codeInput;
  #codeField;
  #error;
  #codeKey;
  #needsCode = false;

  constructor(dialog, roomId, onSubmit) {
    this.#dialog = dialog;
    this.#nameInput = dialog.querySelector('#name');
    this.#codeInput = dialog.querySelector('#code');
    this.#codeField = dialog.querySelector('#code-field');
    this.#error = dialog.querySelector('#join-error');
    this.#codeKey = `syncboard.code.${roomId}`;
    dialog.addEventListener('cancel', (event) => event.preventDefault());
    dialog.querySelector('form').addEventListener('submit', (event) => {
      event.preventDefault();
      this.#remember();
      dialog.close();
      onSubmit();
    });
  }

  get name() {
    return this.#nameInput.value.trim();
  }

  get code() {
    return this.#codeInput.value.trim();
  }

  get isOpen() {
    return this.#dialog.open;
  }

  get isFilledIn() {
    return this.name !== '' && (!this.#needsCode || this.code !== '');
  }

  prepare(room) {
    this.#needsCode = room.locked;
    this.#codeField.hidden = !room.locked;
    this.#codeInput.required = room.locked;
    this.#nameInput.value = load('local', NAME_KEY) ?? '';
    this.#codeInput.value = load('session', this.#codeKey) ?? '';
  }

  ask(error = '') {
    this.#error.textContent = error;
    this.#dialog.showModal();
  }

  askForCode(error) {
    this.#codeField.hidden = false;
    this.ask(error);
  }

  #remember() {
    save('local', NAME_KEY, this.name);
    save('session', this.#codeKey, this.code);
  }
}
