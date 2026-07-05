package org.stevidigital.productcatalog.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.stevidigital.productcatalog.application.command.CreateProductCommand;
import org.stevidigital.productcatalog.application.command.PublishProductCommand;
import org.stevidigital.productcatalog.application.usecase.CreateProductUseCase;
import org.stevidigital.productcatalog.application.usecase.GetProductUseCase;
import org.stevidigital.productcatalog.application.usecase.PublishProductUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * REST ADAPTER — translates HTTP to application-layer commands.
 *
 * Rules: no domain logic, no repository access, no leaking domain exceptions as 500.
 */
@Tag(name = "Product Catalog", description = "Product lifecycle — DRAFT → PUBLISHED → DISCONTINUED")
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final CreateProductUseCase createProduct;
    private final PublishProductUseCase publishProduct;
    private final GetProductUseCase getProduct;

    public ProductController(CreateProductUseCase createProduct,
                             PublishProductUseCase publishProduct,
                             GetProductUseCase getProduct) {
        this.createProduct = createProduct;
        this.publishProduct = publishProduct;
        this.getProduct = getProduct;
    }

    @Operation(summary = "Create a product",
               description = "Creates a product in **DRAFT** status. Fires `ProductCreated` domain event.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Product created",
                     headers = @Header(name = "Location", description = "/api/v1/products/{id}")),
        @ApiResponse(responseCode = "400", description = "Invalid name or unknown category",
                     content = @Content(schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "422", description = "Domain invariant violated",
                     content = @Content(schema = @Schema(type = "string")))
    })
    @PostMapping
    public ResponseEntity<Void> create(@RequestBody @Valid CreateProductRequest request,
                                       UriComponentsBuilder uriBuilder) {
        var id = createProduct.execute(new CreateProductCommand(request.name(), request.category()));
        var location = uriBuilder.path("/api/v1/products/{id}").buildAndExpand(id.value()).toUri();
        return ResponseEntity.created(location).build();
    }

    @Operation(summary = "Get a product by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product found",
                     content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "404", description = "Product not found",
                     content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> findById(
            @Parameter(description = "Product UUID", required = true) @PathVariable String id) {
        return getProduct.findById(id)
                .map(ProductResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "List all products")
    @ApiResponse(responseCode = "200", description = "Product list (may be empty)",
                 content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProductResponse.class))))
    @GetMapping
    public List<ProductResponse> findAll() {
        return getProduct.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Operation(summary = "Publish a product",
               description = "Transitions a product from **DRAFT** to **PUBLISHED**. Fires `ProductPublished` domain event.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Product published"),
        @ApiResponse(responseCode = "400", description = "Product not found",
                     content = @Content(schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "422", description = "Product is not in DRAFT status",
                     content = @Content(schema = @Schema(type = "string")))
    })
    @PutMapping("/{id}/publish")
    public ResponseEntity<Void> publish(
            @Parameter(description = "Product UUID", required = true) @PathVariable String id) {
        publishProduct.execute(new PublishProductCommand(id));
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleInvariantViolation(IllegalStateException ex) {
        return ResponseEntity.unprocessableEntity().body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
