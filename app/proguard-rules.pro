-keepattributes *Annotation*, InnerClasses, Signature

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.characterfinder.**$$serializer { *; }
