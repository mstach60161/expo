#import <ExpoModulesCore/EXUIManager.h>
#import <ExpoModulesCore/EXEventEmitterService.h>
#import <EXAdsAdMob/EXAdsAdMobRewarded.h>
#import <ExpoModulesCore/EXUtilitiesInterface.h>

static NSString *const EXAdsAdMobRewardedUserDidEarnReward = @"rewardedVideoUserDidEarnReward";
static NSString *const EXAdsAdMobRewardedDidLoad = @"rewardedVideoDidLoad";
static NSString *const EXAdsAdMobRewardedDidFailToLoad = @"rewardedVideoDidFailToLoad";
static NSString *const EXAdsAdMobRewardedDidPresent = @"rewardedVideoDidPresent";
static NSString *const EXAdsAdMobRewardedDidFailToPresent = @"rewardedVideoDidFailToPresent";
static NSString *const EXAdsAdMobRewardedDidDismiss = @"rewardedVideoDidDismiss";

@interface EXAdsAdMobRewarded ()

@property (nonatomic, weak) id<EXEventEmitterService> eventEmitter;
@property (nonatomic, weak) id<EXUtilitiesInterface> utilities;
@property (nonatomic, strong) GADRewardedAd *rewardedAd;

@end

@implementation EXAdsAdMobRewarded {
  NSString *_adUnitID;
  BOOL _hasListeners;
  EXPromiseResolveBlock _requestAdResolver;
  EXPromiseRejectBlock _requestAdRejecter;
  EXPromiseResolveBlock _showAdResolver;
}

EX_EXPORT_MODULE(ExpoAdsAdMobRewardedVideoAdManager);

- (void)setModuleRegistry:(EXModuleRegistry *)moduleRegistry
{
  _utilities = [moduleRegistry getModuleImplementingProtocol:@protocol(EXUtilitiesInterface)];
  _eventEmitter = [moduleRegistry getModuleImplementingProtocol:@protocol(EXEventEmitterService)];
}

- (NSArray<NSString *> *)supportedEvents
{
  return @[
    EXAdsAdMobRewardedUserDidEarnReward,
    EXAdsAdMobRewardedDidLoad,
    EXAdsAdMobRewardedDidFailToLoad,
    EXAdsAdMobRewardedDidPresent,
    EXAdsAdMobRewardedDidFailToPresent,
    EXAdsAdMobRewardedDidDismiss,
  ];
}

- (void)startObserving {
  _hasListeners = YES;
}

- (void)_maybeSendEventWithName:(NSString *)name body:(id)body {
  if (_hasListeners) {
    [_eventEmitter sendEventWithName:name body:body];
  }
}

- (void)stopObserving {
  _hasListeners = NO;
}

EX_EXPORT_METHOD_AS(setAdUnitID,
                    setAdUnitID:(NSString *)adUnitID
                    resolver:(EXPromiseResolveBlock)resolve
                    rejecter:(EXPromiseRejectBlock)reject)
{
  _adUnitID = adUnitID;
  resolve(nil);
}

EX_EXPORT_METHOD_AS(requestAd,
                    requestAdWithAdditionalRequestParams:(NSDictionary *)additionalRequestParams
                    resolver:(EXPromiseResolveBlock)resolve
                    rejecter:(EXPromiseRejectBlock)reject)
{
  if (_requestAdRejecter == nil) {
    _requestAdResolver = resolve;
    _requestAdRejecter = reject;
    
    self.rewardedAd = [[GADRewardedAd alloc] initWithAdUnitID:_adUnitID];
    GADRequest *request = [GADRequest request];
    if (additionalRequestParams) {
      GADExtras *extras = [[GADExtras alloc] init];
      extras.additionalParameters = additionalRequestParams;
      [request registerAdNetworkExtras:extras];
    }
    EX_WEAKIFY(self);
    dispatch_async(dispatch_get_main_queue(), ^{
      EX_ENSURE_STRONGIFY(self);
      [self.rewardedAd loadRequest:request
                 completionHandler:^(GADRequestError * _Nullable error) {
        EX_ENSURE_STRONGIFY(self);
        if (error) {
          [self _maybeSendEventWithName:EXAdsAdMobRewardedDidFailToLoad
                                   body:@{ @"name": [error description] }];
          self->_requestAdRejecter(@"E_AD_REQUEST_FAILED", [error description], error);
          [self _cleanupRequestAdPromise];
        } else {
          [self _maybeSendEventWithName:EXAdsAdMobRewardedDidLoad body:nil];
          self->_requestAdResolver(nil);
          [self _cleanupRequestAdPromise];
        }
      }];
    });
  } else {
    reject(@"E_AD_REQUESTING", @"An ad is already being requested, await the previous promise.", nil);
  }
}

EX_EXPORT_METHOD_AS(showAd,
                    showAd:(EXPromiseResolveBlock)resolve
                    rejecter:(EXPromiseRejectBlock)reject)
{
  if (_showAdResolver == nil && self.rewardedAd.isReady) {
    _showAdResolver = resolve;
    EX_WEAKIFY(self);
    dispatch_async(dispatch_get_main_queue(), ^{
      EX_ENSURE_STRONGIFY(self);
      [self.rewardedAd presentFromRootViewController:self.utilities.currentViewController delegate:self];
    });
  } else if (self.rewardedAd.isReady) {
    reject(@"E_AD_BEING_SHOWN", @"Ad is already being shown, await the previous promise.", nil);
  } else {
    reject(@"E_AD_NOT_READY", @"Ad is not ready.", nil);
  }
}

EX_EXPORT_METHOD_AS(dismissAd,
                    dismissAd:(EXPromiseResolveBlock)resolve
                    rejecter:(EXPromiseRejectBlock)reject)
{
  EX_WEAKIFY(self);
  dispatch_async(dispatch_get_main_queue(), ^{
    EX_ENSURE_STRONGIFY(self);
    UIViewController *presentedViewController = self.utilities.currentViewController;
    if (presentedViewController != nil && [NSStringFromClass([presentedViewController class]) isEqualToString:@"GADInterstitialViewController"]) {
      [presentedViewController dismissViewControllerAnimated:true completion:^{
        resolve(nil);
      }];
    } else {
      reject(@"E_AD_NOT_SHOWN", @"Ad is not being shown.", nil);
    }
  });
}

EX_EXPORT_METHOD_AS(getIsReady,
                    getIsReady:(EXPromiseResolveBlock)resolve
                    rejecter:(EXPromiseRejectBlock)reject)
{
  resolve([NSNumber numberWithBool:self.rewardedAd.isReady]);
}


- (void)_cleanupRequestAdPromise
{
  _requestAdResolver = nil;
  _requestAdRejecter = nil;
}

- (void)rewardedAd:(GADRewardedAd *)rewardedAd userDidEarnReward:(GADAdReward *)reward {
  [self _maybeSendEventWithName:EXAdsAdMobRewardedUserDidEarnReward
                           body:@{ @"type": reward.type, @"amount": reward.amount }];
}

- (void)rewardedAdDidPresent:(GADRewardedAd *)rewardedAd {
  [self _maybeSendEventWithName:EXAdsAdMobRewardedDidPresent body:nil];
  _showAdResolver(nil);
  _showAdResolver = nil;
}

- (void)rewardedAd:(GADRewardedAd *)rewardedAd didFailToPresentWithError:(NSError *)error
{
  [self _maybeSendEventWithName:EXAdsAdMobRewardedDidFailToPresent
                           body:@{ @"name": [error description] }];
  _requestAdRejecter(@"E_AD_REQUEST_FAILED", [error description], error);
  [self _cleanupRequestAdPromise];
}

- (void)rewardedAdDidDismiss:(GADRewardedAd *)rewardedAd {
  [self _maybeSendEventWithName:EXAdsAdMobRewardedDidDismiss body:nil];
}

@end

