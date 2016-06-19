package at.htl.jpa.locking.entity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String description;

    @NotNull
    private float price;

    @Version
    private int version;

    public Product() {
    }

    public Product(String description, float price) {
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

    public int getVersion() {
        return version;
    }


    @Override
    public String toString() {
        return String.format("%3s: %s, %.2f, Version %d",
                (id == null ? "n/a" : String.valueOf(id)),
                description,
                price
                ,version
        );
    }
}
