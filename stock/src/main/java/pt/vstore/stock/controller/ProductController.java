package pt.vstore.stock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pt.vstore.stock.dto.Product;
import pt.vstore.stock.service.ProductService;

import java.util.Collection;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/product")
@Tag(name = "Product", description = "The product API v1" )
public class ProductController {

    @Autowired
    private ProductService service;


    @GetMapping("/{id}")
    @Operation(summary = "Get a product by its id")
    public Product findById(
            @Parameter(description = "id of the product") @PathVariable UUID id) {

        return service.findById(id);
    }

    @GetMapping("/list")
    @Operation(summary = "Get a list of products")
    public Collection<Product> findProducts() {
        return service.findProducts();
    }

    @GetMapping("/filter")
    @Operation(summary = "Filter products")
    public Page<Product> filterProducts(Pageable pageable) {
        return service.findProducts(pageable);
    }


    @PostMapping("/")
    @Operation(summary = "Create a product")
    @ResponseStatus(HttpStatus.CREATED)
    public Product createProduct(@RequestBody final Product prod) {
        return service.createProduct(prod);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateProduct(@PathVariable("id") final UUID id,
                                    @RequestBody final Product prod) {

        prod.setId(id);
        service.updateProduct(prod);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable("id") final UUID id) {
        service.deleteProduct(id);
    }
}
