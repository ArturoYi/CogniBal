-- Room AppDatabase schema 1. Split on semicolons. Do not reformat.
CREATE TABLE IF NOT EXISTS `account` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, `color` TEXT NOT NULL, `type` INTEGER NOT NULL, `balance` INTEGER NOT NULL, `isEnable` INTEGER NOT NULL, `remark` TEXT, `createTime` TEXT NOT NULL, `updateTime` TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS `category` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, `color` TEXT NOT NULL, `type` INTEGER NOT NULL, `sort` INTEGER NOT NULL, `isEnable` INTEGER NOT NULL);
CREATE INDEX IF NOT EXISTS `index_category_type_sort` ON `category` (`type`, `sort`);
CREATE TABLE IF NOT EXISTS `bill` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount` INTEGER NOT NULL, `type` INTEGER NOT NULL, `categoryId` INTEGER, `accountId` INTEGER NOT NULL, `billTime` TEXT NOT NULL, `remark` TEXT, `createTime` TEXT NOT NULL, `updateTime` TEXT NOT NULL, FOREIGN KEY(`accountId`) REFERENCES `account`(`id`) ON UPDATE RESTRICT ON DELETE RESTRICT , FOREIGN KEY(`categoryId`) REFERENCES `category`(`id`) ON UPDATE RESTRICT ON DELETE RESTRICT );
CREATE INDEX IF NOT EXISTS `index_bill_billTime` ON `bill` (`billTime`);
CREATE INDEX IF NOT EXISTS `index_bill_accountId` ON `bill` (`accountId`);
CREATE INDEX IF NOT EXISTS `index_bill_categoryId` ON `bill` (`categoryId`);
CREATE INDEX IF NOT EXISTS `index_bill_type_billTime` ON `bill` (`type`, `billTime`);
CREATE INDEX IF NOT EXISTS `index_bill_type_createTime` ON `bill` (`type`, `createTime`);
CREATE TABLE IF NOT EXISTS `budget` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `budgetType` INTEGER NOT NULL, `categoryId` INTEGER, `limitAmount` INTEGER NOT NULL, `alertPercent` INTEGER NOT NULL, `periodType` INTEGER NOT NULL, `periodStart` TEXT NOT NULL, `periodEnd` TEXT NOT NULL, `isEnable` INTEGER NOT NULL, `remark` TEXT, `createTime` TEXT NOT NULL, `updateTime` TEXT NOT NULL, FOREIGN KEY(`categoryId`) REFERENCES `category`(`id`) ON UPDATE RESTRICT ON DELETE RESTRICT );
CREATE INDEX IF NOT EXISTS `index_budget_categoryId` ON `budget` (`categoryId`);
CREATE INDEX IF NOT EXISTS `index_budget_periodStart_periodEnd` ON `budget` (`periodStart`, `periodEnd`);
