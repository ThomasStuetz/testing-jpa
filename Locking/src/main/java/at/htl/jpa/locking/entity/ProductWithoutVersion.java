package at.htl.jpa.locking.entity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "PRODUCT_WO_VERS")
public class ProductWithoutVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String description;

    @NotNull
    private float price;

//    @Version
//    private int version;

    public ProductWithoutVersion() {
    }

    public ProductWithoutVersion(String description, float price) {
        this.description = description;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return String.format("%3s: %s, %.2f, Version n/a",
                (id == null ? "n/a" : String.valueOf(id)),
                description,
                price
        );
    }

}
