// swift-tools-version: 6.0

import PackageDescription

let package = Package(
    name: "CogniBalLedger",
    platforms: [
        .iOS(.v16),
        .macOS(.v13),
    ],
    products: [
        .library(name: "CogniBalLedger", targets: ["CogniBalLedger"]),
    ],
    dependencies: [
        .package(url: "https://github.com/groue/GRDB.swift.git", from: "7.12.0"),
    ],
    targets: [
        .target(
            name: "CogniBalLedger",
            dependencies: [
                .product(name: "GRDB", package: "GRDB.swift"),
            ],
            resources: [
                .copy("SchemaSQL"),
            ]
        ),
        .testTarget(
            name: "CogniBalLedgerTests",
            dependencies: ["CogniBalLedger"],
            exclude: ["Fixtures"]
        ),
    ]
)
