import { describe, expect, it } from 'vitest';
import {
  esCedulaEcuatorianaValida,
  esContrasenaValida,
  esEmailValido,
  esTelefonoEcuatorianoValido
} from './identidad';

// Mismos casos que firebase/functions/src/utils/validaciones.test.ts (paridad cliente-servidor).
describe('Validadores de identidad (paridad con el servidor)', () => {
  it('cédula: acepta válidas y rechaza longitud, provincia, tercer dígito y verificador inválidos', () => {
    expect(esCedulaEcuatorianaValida('1710034065')).toBe(true);
    expect(esCedulaEcuatorianaValida('0926687856')).toBe(true);
    expect(esCedulaEcuatorianaValida('171003406')).toBe(false);
    expect(esCedulaEcuatorianaValida('171003406a')).toBe(false);
    expect(esCedulaEcuatorianaValida('2510034065')).toBe(false);
    expect(esCedulaEcuatorianaValida('0010034065')).toBe(false);
    expect(esCedulaEcuatorianaValida('1760034065')).toBe(false);
    expect(esCedulaEcuatorianaValida('1710034064')).toBe(false);
  });

  it('cédula: acepta la provincia 30 (ecuatorianos en el exterior) si el verificador cuadra', () => {
    // 3·2 + 0·1 + 1·2 + 2·1 + 3·2 + 4·1 + (5·2−9) + 6·1 + (7·2−9) = 32 → verificador 10 − 2 = 8
    expect(esCedulaEcuatorianaValida('3012345678')).toBe(true);
    expect(esCedulaEcuatorianaValida('3012345677')).toBe(false);
  });

  it('correo', () => {
    expect(esEmailValido('admin@silmay.ec')).toBe(true);
    expect(esEmailValido('correo@dominio')).toBe(false);
    expect(esEmailValido('sin-arroba')).toBe(false);
  });

  it('teléfono ecuatoriano', () => {
    expect(esTelefonoEcuatorianoValido('0991234567')).toBe(true);
    expect(esTelefonoEcuatorianoValido('+593991234567')).toBe(true);
    expect(esTelefonoEcuatorianoValido('032740123')).toBe(true);
    expect(esTelefonoEcuatorianoValido('099 123-4567')).toBe(true);
    expect(esTelefonoEcuatorianoValido('0891234567')).toBe(false);
    expect(esTelefonoEcuatorianoValido('12345')).toBe(false);
  });

  it('contraseña: ≥ 8 con letras y números; sin espacios en los extremos', () => {
    expect(esContrasenaValida('Silmay2026!Admin')).toBe(true);
    expect(esContrasenaValida('Silmay26')).toBe(true);
    expect(esContrasenaValida('Abc1234')).toBe(false);
    expect(esContrasenaValida('SoloLetras')).toBe(false);
    expect(esContrasenaValida('12345678')).toBe(false);
    expect(esContrasenaValida(' Silmay2026')).toBe(false);
  });
});
