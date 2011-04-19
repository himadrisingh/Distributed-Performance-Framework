/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.springframework.samples.petclinic;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.Serializable;

/**
 * Simple JavaBean domain object with an id property. Used as a base class for objects needing this property.
 * 
 * @author Ken Krebs
 * @author Juergen Hoeller
 */
public class BaseEntity implements Serializable {

  private Integer id;

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getId() {
    return id;
  }

  public boolean isNew() {
    return (this.id == null);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(getId())
        .toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof BaseEntity) {
      BaseEntity other = (BaseEntity) obj;

      return new EqualsBuilder()
          .append(getId(), other.getId())
          .isEquals();
    }
    return false;
  }

}
