/*
 * EpicGuard is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EpicGuard is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package me.xneox.epicguard.core.storage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.net.InetAddresses;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import me.xneox.epicguard.core.EpicGuard;
import me.xneox.epicguard.core.user.ConnectingUser;
import me.xneox.epicguard.core.util.LogUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class caches all known {@link AddressMeta}'s and performs various operations on them.
 */
public class StorageManager {
  private final Cache<String, AddressMeta> addresses = Caffeine.newBuilder().build();
  private final Collection<String> pingCache = new HashSet<>();
  private final Database database;

  public StorageManager(EpicGuard epicGuard) {
    this.database = new Database(epicGuard);
  }

  public void setupDatabase() {
    try {
      this.database.connect();
      this.database.load();
    } catch (Exception exception) {
      LogUtils.catchException("Could not connect to the database. Check if your connection is configured correctly.", exception);
    }
  }

  /**
   * Returns an {@link AddressMeta} for the specified address. Creates a new AddressMeta if it
   * doesn't exist for this address.
   */
  @NotNull
  public AddressMeta addressMeta(final @NotNull String address) {
    return this.addresses.get(address, s -> new AddressMeta(false, false, new ArrayList<>(), true));
  }

  /**
   * When an address is specified:
   * <p>- Redirects to the {@link #addressMeta(String)} method.</p>
   * <p>- Never returns null.</p>
   * <p>
   * <p>When a nickname is specified:</p>
   * <p>- Tries to detect last used address by this nickname.
   * <p>- If found, redirects to the {@link #addressMeta(String)} method.
   * <p>- If not found, returns null.
   */
  @Nullable
  public AddressMeta resolveAddressMeta(@NotNull String value) {
    final String address = InetAddresses.isInetAddress(value) ? value : lastSeenAddress(value);
    return address != null ? addressMeta(address) : null;
  }

  /**
   * Searches for the last used address of the specified nickname.
   */
  @Nullable
  public String lastSeenAddress(@NotNull String nickname) {
    return this.addresses.asMap().entrySet().stream()
        .filter(entry -> entry.getValue().nicknames().stream().anyMatch(nick -> nick.equalsIgnoreCase(nickname)))
        .findFirst()
        .map(Map.Entry::getKey)
        .orElse(null);
  }

  /**
   * Checks if the address meta of connecting user contains his current nickname.
   * If absent, it is added.
   */
  public void updateAccounts(@NotNull ConnectingUser user) {
    final var meta = this.addressMeta(user.address());
    if (!meta.nicknames().contains(user.nickname())) {
      meta.addNickname(user.nickname());
    }
  }

  /**
   * A legacy method for viewing addresses that meet a specific condition. For example, this can
   * return whitelisted or blacklisted addresses.
   * <p></p>
   * <p>Returned list is immutable, used only for statistics and command suggestions.</p>
   */
  @NotNull
  public List<String> viewAddresses(@NotNull Predicate<AddressMeta> predicate) {
    return this.addresses.asMap().entrySet().stream()
        .filter(entry -> predicate.test(entry.getValue()))
        .map(Map.Entry::getKey)
        .toList();
  }

  @NotNull
  public Cache<String, AddressMeta> addresses() {
    return this.addresses;
  }

  @NotNull
  public Database database() {
    return this.database;
  }

  @NotNull
  public Collection<String> pingCache() {
    return this.pingCache;
  }
}
