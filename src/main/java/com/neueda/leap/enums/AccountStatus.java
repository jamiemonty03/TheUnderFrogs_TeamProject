package com.neueda.leap.enums;

/**
 * Account status enumeration.
 * 
 * ACTIVE: Account can execute trades
 * INACTIVE: Account cannot trade but can still be viewed
 * SUSPENDED: Account is frozen, no trades or withdrawals allowed
 */
public enum AccountStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED
}
