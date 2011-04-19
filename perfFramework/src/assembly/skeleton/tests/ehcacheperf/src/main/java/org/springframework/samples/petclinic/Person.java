/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.springframework.samples.petclinic;

/**
 * Simple JavaBean domain object representing an person.
 * 
 * @author Ken Krebs
 */
public class Person extends BaseEntity {

  private String firstName;

  private String lastName;

  public String getFirstName() {
    return this.firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return this.lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

}
