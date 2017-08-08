package org.pgp;

import java.util.Objects;

public final class Gym implements Comparable<Gym> {
  private final long gymId;
  private final String gymName;

  private final GymInfo gymInfo;

  public Gym(final long gymId, final String gymName) {
    this(gymId, gymName, null);
  }

  public Gym(final long gymId, final String gymName, final GymInfo gymInfo) {
    this.gymId = gymId;
    this.gymName = gymName;
    this.gymInfo = gymInfo;
  }

  public Gym withGymInfo(final GymInfo gymInfo) {
    return new Gym(gymId, gymName, gymInfo);
  }

  public long getGymId() {
    return gymId;
  }

  public String getGymName() {
    return gymName;
  }

  public GymInfo getGymInfo() {
    return gymInfo;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj instanceof Gym) {
      final Gym other = (Gym) obj;

      return Objects.equals(gymId, other.gymId) &&
          Objects.equals(gymName, other.gymName) &&
          Objects.equals(gymInfo, other.gymInfo);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(gymId, gymName, gymInfo);
  }

  @Override
  public int compareTo(final Gym other) {
    return Math.toIntExact(gymId - other.gymId);
  }
}
