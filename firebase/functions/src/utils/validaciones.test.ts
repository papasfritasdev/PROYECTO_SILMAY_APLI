import { describe, it, expect } from "vitest";
import {
  esCedulaEcuatorianaValida,
  esRolValido,
  esEmailValido,
  esContrasenaValida,
  esTelefonoEcuatorianoValido,
  ROLES_VALIDOS
} from "./validaciones";

describe("Validaciones de Negocio SILMAY", () => {
  describe("esCedulaEcuatorianaValida (Algoritmo Módulo 10)", () => {
    it("debe aceptar cédulas ecuatorianas legítimas", () => {
      expect(esCedulaEcuatorianaValida("1710034065")).toBe(true);
      expect(esCedulaEcuatorianaValida("0926687856")).toBe(true);
    });

    it("debe rechazar cédulas con longitud distinta a 10 dígitos", () => {
      expect(esCedulaEcuatorianaValida("171003406")).toBe(false);
      expect(esCedulaEcuatorianaValida("17100340655")).toBe(false);
      expect(esCedulaEcuatorianaValida("")).toBe(false);
    });

    it("debe rechazar cadenas no numéricas", () => {
      expect(esCedulaEcuatorianaValida("171003406a")).toBe(false);
      expect(esCedulaEcuatorianaValida("abcdefghij")).toBe(false);
      expect(esCedulaEcuatorianaValida(null as unknown as string)).toBe(false);
      expect(esCedulaEcuatorianaValida(undefined as unknown as string)).toBe(false);
    });

    it("debe rechazar códigos de provincia fuera de rango (1-24)", () => {
      // Provincia 00 o 25+
      expect(esCedulaEcuatorianaValida("0010034065")).toBe(false);
      expect(esCedulaEcuatorianaValida("2510034065")).toBe(false);
      expect(esCedulaEcuatorianaValida("9910034065")).toBe(false);
    });

    it("debe rechazar tercer dígito mayor o igual a 6 para personas naturales", () => {
      // Provincia 17, pero tercer dígito 6 o 9
      expect(esCedulaEcuatorianaValida("1760034065")).toBe(false);
      expect(esCedulaEcuatorianaValida("1790034065")).toBe(false);
    });

    it("debe rechazar cédulas con dígito verificador incorrecto", () => {
      // 1710034065 es válida con 5; probamos con 4 y 6
      expect(esCedulaEcuatorianaValida("1710034064")).toBe(false);
      expect(esCedulaEcuatorianaValida("1710034066")).toBe(false);
    });
  });

  describe("esRolValido (RBAC SILMAY)", () => {
    it("debe permitir los 4 roles canónicos del sistema", () => {
      expect(ROLES_VALIDOS).toEqual(["operador", "supervisor", "contador", "admin"]);
      expect(esRolValido("operador")).toBe(true);
      expect(esRolValido("supervisor")).toBe(true);
      expect(esRolValido("contador")).toBe(true);
      expect(esRolValido("admin")).toBe(true);
    });

    it("debe rechazar roles inexistentes o maliciosos", () => {
      expect(esRolValido("gerente")).toBe(false);
      expect(esRolValido("superadmin")).toBe(false);
      expect(esRolValido("ADMIN")).toBe(false);
      expect(esRolValido("")).toBe(false);
      expect(esRolValido(123 as unknown as string)).toBe(false);
      expect(esRolValido(null as unknown as string)).toBe(false);
    });
  });

  describe("esEmailValido", () => {
    it("debe aceptar correos con estructura válida", () => {
      expect(esEmailValido("admin@silmay.ec")).toBe(true);
      expect(esEmailValido("operador.cantera@silmay.com")).toBe(true);
    });

    it("debe rechazar correos con formato inválido", () => {
      expect(esEmailValido("correo_sin_arroba")).toBe(false);
      expect(esEmailValido("correo@dominio")).toBe(false);
      expect(esEmailValido("")).toBe(false);
    });
  });
  describe("esContrasenaValida (HU-02 CA1)", () => {
    it("acepta 8+ caracteres con letras y números, con o sin símbolos", () => {
      expect(esContrasenaValida("Silmay26")).toBe(true);
      expect(esContrasenaValida("Silmay2026!Admin")).toBe(true);
      expect(esContrasenaValida("contraseña1")).toBe(true);
    });

    it("rechaza contraseñas cortas, sin letras o sin dígitos", () => {
      expect(esContrasenaValida("Abc1234")).toBe(false);
      expect(esContrasenaValida("12345678")).toBe(false);
      expect(esContrasenaValida("SoloLetras")).toBe(false);
      expect(esContrasenaValida("!!!!!!!!")).toBe(false);
    });

    it("rechaza espacios en los extremos y valores no string", () => {
      expect(esContrasenaValida(" Silmay2026")).toBe(false);
      expect(esContrasenaValida("Silmay2026 ")).toBe(false);
      expect(esContrasenaValida(undefined)).toBe(false);
      expect(esContrasenaValida(12345678)).toBe(false);
    });
  });

  describe("esTelefonoEcuatorianoValido", () => {
    it("acepta celulares y fijos en formato nacional e internacional", () => {
      expect(esTelefonoEcuatorianoValido("0991234567")).toBe(true);
      expect(esTelefonoEcuatorianoValido("+593991234567")).toBe(true);
      expect(esTelefonoEcuatorianoValido("032740123")).toBe(true);
      expect(esTelefonoEcuatorianoValido("+59332740123")).toBe(true);
      expect(esTelefonoEcuatorianoValido("099 123-4567")).toBe(true);
    });

    it("rechaza longitudes o prefijos inválidos", () => {
      expect(esTelefonoEcuatorianoValido("099123456")).toBe(false);
      expect(esTelefonoEcuatorianoValido("0891234567")).toBe(false);
      expect(esTelefonoEcuatorianoValido("+5730012345678")).toBe(false);
      expect(esTelefonoEcuatorianoValido("")).toBe(false);
      expect(esTelefonoEcuatorianoValido(null)).toBe(false);
    });
  });
});
