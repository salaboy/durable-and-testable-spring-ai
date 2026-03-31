package com.salaboy.warehouse;

import java.math.BigDecimal;

public record Item(String id, String name, int quantity, String description, BigDecimal pricePerUnit) {
}
