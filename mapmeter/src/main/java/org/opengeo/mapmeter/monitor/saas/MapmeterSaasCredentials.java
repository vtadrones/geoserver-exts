package org.opengeo.mapmeter.monitor.saas;

import com.google.common.base.Objects;

public class MapmeterSaasCredentials {

    private final String username;

    private final String password;

    public MapmeterSaasCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(MapmeterSaasCredentials.class)
                .add("username", username)
                .add("password", password)
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MapmeterSaasCredentials)) {
            return false;
        }
        MapmeterSaasCredentials that = (MapmeterSaasCredentials) obj;
        return Objects.equal(this.username, that.username)
                && Objects.equal(this.password, that.password);
    }

}
