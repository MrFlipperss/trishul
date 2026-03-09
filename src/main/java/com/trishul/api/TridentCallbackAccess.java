package com.trishul.api;

public interface TridentCallbackAccess {
    void trishul$freeze();
    void trishul$unfreeze();
    boolean trishul$isFrozen();
}