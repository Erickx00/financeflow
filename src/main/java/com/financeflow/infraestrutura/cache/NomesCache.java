package com.financeflow.infraestrutura.cache;

import java.util.List;

public final class NomesCache {

    public static final String REPORT_SUMMARY = "ff:report:summary";
    public static final String REPORT_CATEGORIES = "ff:report:categories";
    public static final String REPORT_CASH_FLOW = "ff:report:cash-flow";

    public static final List<String> ALL = List.of(REPORT_SUMMARY, REPORT_CATEGORIES, REPORT_CASH_FLOW);

    private NomesCache() {
    }
}
