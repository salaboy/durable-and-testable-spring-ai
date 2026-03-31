package com.salaboy.warehouse;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/items")
public class ItemController {

  private final Map<String, Item> items = new ConcurrentHashMap<>();

  public ItemController() {
    List<Item> mockItems = List.of(
        new Item("cpu-001", "AMD Ryzen 9 7950X", 25, "16-core, 32-thread desktop processor, 5.7GHz max boost", new BigDecimal("549.99")),
        new Item("cpu-002", "Intel Core i7-14700K", 30, "20-core, 28-thread desktop processor, 5.6GHz max boost", new BigDecimal("399.99")),
        new Item("gpu-001", "NVIDIA GeForce RTX 4090", 10, "24GB GDDR6X graphics card, Ada Lovelace architecture", new BigDecimal("1599.99")),
        new Item("gpu-002", "AMD Radeon RX 7900 XTX", 15, "24GB GDDR6 graphics card, RDNA 3 architecture", new BigDecimal("899.99")),
        new Item("ram-001", "Corsair Vengeance DDR5-6000 32GB", 50, "2x16GB DDR5 RAM kit, CL36, Intel XMP 3.0", new BigDecimal("109.99")),
        new Item("ram-002", "G.Skill Trident Z5 DDR5-6400 64GB", 20, "2x32GB DDR5 RAM kit, CL32, RGB", new BigDecimal("229.99")),
        new Item("ssd-001", "Samsung 990 Pro 2TB", 40, "PCIe 4.0 NVMe M.2 SSD, 7450MB/s read", new BigDecimal("169.99")),
        new Item("ssd-002", "WD Black SN850X 1TB", 35, "PCIe 4.0 NVMe M.2 SSD, 7300MB/s read", new BigDecimal("89.99")),
        new Item("mb-001", "ASUS ROG Strix Z790-E", 18, "Intel LGA 1700 ATX motherboard, DDR5, WiFi 6E", new BigDecimal("379.99")),
        new Item("mb-002", "MSI MAG X670E Tomahawk", 22, "AMD AM5 ATX motherboard, DDR5, WiFi 6E", new BigDecimal("299.99")),
        new Item("psu-001", "Corsair RM1000x", 30, "1000W 80+ Gold fully modular ATX power supply", new BigDecimal("189.99")),
        new Item("case-001", "Fractal Design Torrent", 15, "Mid-tower ATX case, open grille design, 2x180mm fans", new BigDecimal("189.99")),
        new Item("cool-001", "Noctua NH-D15", 28, "Dual-tower CPU air cooler, 2x140mm fans, 250W TDP", new BigDecimal("109.99")),
        new Item("cool-002", "Corsair iCUE H150i Elite", 20, "360mm AIO liquid CPU cooler, RGB pump head", new BigDecimal("169.99"))
    );
    mockItems.forEach(item -> items.put(item.id(), item));
  }

  @GetMapping
  public Collection<Item> getAll() {
    return items.values();
  }

  @GetMapping("/{id}")
  public Item getById(@PathVariable String id) {
    Item item = items.get(id);
    if (item == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + id);
    }
    return item;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Item create(@RequestBody Item item) {
    if (items.containsKey(item.id())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Item already exists: " + item.id());
    }
    items.put(item.id(), item);
    return item;
  }

  @PutMapping("/{id}")
  public Item update(@PathVariable String id, @RequestBody Item item) {
    if (!items.containsKey(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + id);
    }
    Item updated = new Item(id, item.name(), item.quantity(), item.description(), item.pricePerUnit());
    items.put(id, updated);
    return updated;
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    if (items.remove(id) == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + id);
    }
  }
}
