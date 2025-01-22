package com.sensa.usermanagementservice.data.entity;

import com.sensa.usermanagementservice.data.enums.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@Embeddable
@Getter
@Setter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdditionalUserInfo {
    @Column(name = "gender")
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "blood_group")
    @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Invalid blood group")
    private String bloodGroup;

    @Column(name = "height")
    @Min(value = 50, message = "Height must be at least 50 cm")
    @Max(value = 300, message = "Height cannot exceed 300 cm")
    private Double height;

    @Column(name = "country")
    @Size(max = 100, message = "Country name cannot exceed 100 characters")
    private String country;

    @Column(name = "city")
    @Size(max = 100, message = "City name cannot exceed 100 characters")
    private String city;


    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        AdditionalUserInfo that = (AdditionalUserInfo) o;
        return getGender() != null && Objects.equals(getGender(), that.getGender())
                && getBloodGroup() != null && Objects.equals(getBloodGroup(), that.getBloodGroup())
                && getHeight() != null && Objects.equals(getHeight(), that.getHeight())
                && getCountry() != null && Objects.equals(getCountry(), that.getCountry())
                && getCity() != null && Objects.equals(getCity(), that.getCity());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(gender,
                bloodGroup,
                height,
                country,
                city);
    }
}
