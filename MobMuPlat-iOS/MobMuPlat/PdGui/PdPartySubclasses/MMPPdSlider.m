//
//  MMPPdSlider.m
//  MobMuPlat
//
//  Created by diglesia on 1/18/16.
//  Copyright © 2016 Daniel Iglesia. All rights reserved.
//

#import "MMPPdSlider.h"

@implementation MMPPdSlider


#pragma mark WidgetListener

- (void)receiveBangFromSource:(NSString *)source {
  //[self sendFloat:self.value];
}

- (void)receiveFloat:(float)received fromSource:(NSString *)source {
  self.value = received;
  //[super sendFloat:received]; // Pd 0.46+ doesn't clip incoming values
}

@end
