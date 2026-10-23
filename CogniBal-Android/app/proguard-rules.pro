# zip4j 的 AES 解密会按类型查找自己的实现。收成库之后仍要留着这些类。
-keep class net.lingala.zip4j.** { *; }
-dontwarn net.lingala.zip4j.**
-dontwarn org.bouncycastle.**
