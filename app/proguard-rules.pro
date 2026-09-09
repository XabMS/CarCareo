# R8 is on for release (isMinifyEnabled + isShrinkResources in build.gradle.kts).
# Most libraries here (Compose, Room, Navigation, Lifecycle, WorkManager,
# kotlinx.serialization) ship their own consumer rules; the keeps below cover the
# few app-specific things R8 cannot see through.

# --- Backup file format: round-trips through reflection-driven serialization ---
# The kotlinx.serialization plugin ships keep rules, but pin our model package
# and its generated $serializer classes explicitly.
-keepclassmembers @kotlinx.serialization.Serializable class com.xabier.carcareo.data.backup.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.xabier.carcareo.data.backup.**$$serializer { *; }

# --- Enums that travel in the backup file as their constant name ---
# BackupCodec / BackupRepository call VehicleCategory.valueOf(string); the
# constant names must not be obfuscated or the enum unboxed.
-keepclassmembers enum com.xabier.carcareo.data.entity.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- WorkManager instantiates these by class name via its default factory ---
-keep class com.xabier.carcareo.notifications.**Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
