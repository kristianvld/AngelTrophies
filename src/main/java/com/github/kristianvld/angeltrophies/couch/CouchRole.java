package com.github.kristianvld.angeltrophies.couch;

public enum CouchRole {

    Single, RightEnd, LeftEnd, Middle, InnerCorner, OuterCorner;

    public static CouchRole parse(String name) {
        for (CouchRole role : values()) {
            if (role.name().equalsIgnoreCase(name)) {
                return role;
            }
        }
        return null;
    }
}
