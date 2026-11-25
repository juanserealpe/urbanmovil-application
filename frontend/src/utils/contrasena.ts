// Sin caracteres ambiguos (0/O, 1/l/I) para que se pueda dictar o copiar a mano.
const MINUSCULAS = 'abcdefghijkmnpqrstuvwxyz';
const MAYUSCULAS = 'ABCDEFGHJKLMNPQRSTUVWXYZ';
const DIGITOS = '23456789';
const TODOS = MINUSCULAS + MAYUSCULAS + DIGITOS;

function aleatorio(max: number): number {
  const arr = new Uint32Array(1);
  crypto.getRandomValues(arr);
  return arr[0] % max;
}

function elegir(conjunto: string): string {
  return conjunto[aleatorio(conjunto.length)];
}

/** Contraseña aleatoria con al menos una minúscula, una mayúscula y un dígito. */
export function generarContrasena(longitud = 12): string {
  const chars = [elegir(MINUSCULAS), elegir(MAYUSCULAS), elegir(DIGITOS)];
  while (chars.length < longitud) chars.push(elegir(TODOS));
  // Fisher–Yates para que los obligatorios no queden siempre al principio.
  for (let i = chars.length - 1; i > 0; i--) {
    const j = aleatorio(i + 1);
    [chars[i], chars[j]] = [chars[j], chars[i]];
  }
  return chars.join('');
}
