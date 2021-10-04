//  Copyright © 2021 650 Industries. All rights reserved.

#import <ABI43_0_0EXUpdates/ABI43_0_0EXUpdatesDatabaseMigration.h>

NS_ASSUME_NONNULL_BEGIN

@interface ABI43_0_0EXUpdatesDatabaseMigrationRegistry : NSObject

+ (NSArray<id<ABI43_0_0EXUpdatesDatabaseMigration>> *)migrations;

@end

NS_ASSUME_NONNULL_END

