package com.rbownworkshop.server.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppConfig {

    @Id
    @Column(name = "config_key", length = 100)
    private String key;

    @Column(name = "config_value", length = 500)
    private String value;
}