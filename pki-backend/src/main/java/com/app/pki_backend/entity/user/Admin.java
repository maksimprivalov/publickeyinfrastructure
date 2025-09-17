package com.app.pki_backend.entity.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;

@Data
@Entity
@DiscriminatorValue("Admin")
public class Admin extends User{
}
