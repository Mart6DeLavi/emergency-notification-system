package com.sensa.usermanagementservice.data.entity;

import com.sensa.usermanagementservice.data.enums.PreferredCommunicationChannel;
import com.sensa.usermanagementservice.data.enums.Role;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Entity(name = "client")
@Getter
@Setter
@RequiredArgsConstructor
@DynamicUpdate
@Table(name = "clients", uniqueConstraints = {@UniqueConstraint(columnNames = {"username", "email", "phoneNumber"})})
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "clients_seq_gen")
    @SequenceGenerator(name = "clients_seq_gen", sequenceName = "clients_seq", allocationSize = 1)
    private Long id;

    @Column(name = "name", nullable = false)
    @NotBlank(message = "Name cannot be blank")
    @Size(max = 50, message = "Name cannot exceed 50 characters")
    private String name;

    @Column(name = "second_name", nullable = false)
    @NotBlank(message = "Second name cannot be blank")
    @Size(max = 50, message = "Second name cannot exceed 50 characters")
    private String secondName;

    @Column(name = "username",
            unique = true,
            nullable = false)
    @NotBlank(message = "Username cannot be blank")
    private String username;

    @Column(name = "password", nullable = false)
    @NotBlank(message = "Password cannot be blank")
    private String password;

    @Column(name = "email",
            unique = true,
            nullable = false)
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email cannot be blank")
    private String email;

    @Column(name = "phone_number",
            unique = true,
            nullable = false)
    @NotBlank(message = "Phone cannot be blank")
    @Pattern(regexp = "\\+?[0-9]{10,15}", message = "Invalid phone number format")
    private String phoneNumber;

    @Column(name = "age", nullable = false)
    @NotNull(message = "Age cannot be null")
    @Min(value = 0, message = "Age cannot be negative")
    @Max(value = 150, message = "Age cannot exceed 150")
    private Integer age;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "preferred_communication_channel")
    @Enumerated(EnumType.STRING)
    private PreferredCommunicationChannel preferredCommunicationChannel;

    @Column(name = "creation_date")
    @CreationTimestamp
    private LocalDateTime creationDate;

    @Column(name = "date_of_update")
    @UpdateTimestamp
    private LocalDateTime dateOfUpdate;

    @Embedded
    private AdditionalUserInfo additionalUserInfo;

    @OneToOne(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private SystemData systemData;

    @OneToMany(mappedBy = "client", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @BatchSize(size = 10)
    @Valid
    private List<EmergencyContact> emergencyContactList;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Client client = (Client) o;
        return getId() != null && Objects.equals(getId(), client.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
