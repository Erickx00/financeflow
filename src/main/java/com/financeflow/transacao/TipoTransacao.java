package com.financeflow.transacao;

/**
 * Direcao do fluxo financeiro. Usado tanto por transacoes quanto por
 * categorias: o banco garante (FK composta em V4) que os dois coincidem.
 */
public enum TipoTransacao {
    INCOME,
    EXPENSE
}
