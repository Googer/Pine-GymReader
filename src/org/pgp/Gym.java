package org.pgp;

import java.util.Objects;

public final class Gym implements Comparable<Gym> {
  private long gymId;
  private String gymName;

  private GymInfo gymInfo;

  public Gym() {
  }

  public Gym(final long gymId, final String gymName) {
    this(gymId, gymName, null);
  }

  public Gym(final long gymId, final String gymName, final GymInfo gymInfo) {
    this.gymId = gymId;
    this.gymName = gymName;
    this.gymInfo = gymInfo;
  }

  public long getGymId() {
    return gymId;
  }

  public void setGymId(final long gymId) {
    this.gymId = gymId;
  }

  public String getGymName() {
    return gymName;
  }

  public void setGymName(final String gymName) {
    this.gymName = gymName;
  }

  public GymInfo getGymInfo() {
    return gymInfo;
  }

  public void setGymInfo(final GymInfo gymInfo) {
    this.gymInfo = gymInfo;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj instanceof Gym) {
      final Gym other = (Gym) obj;

      return Objects.equals(gymId, other.gymId) &&
          Objects.equals(gymName, other.gymName);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(gymId, gymName);
  }

  @Override
  public int compareTo(final Gym other) {
    return Math.toIntExact(gymId - other.gymId);
  }
}
