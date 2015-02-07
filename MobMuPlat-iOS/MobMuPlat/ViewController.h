//
//  ViewController.h
//  MobMuPlat
//
//  Created by Daniel Iglesia on 11/15/12.
//  Copyright (c) 2012 Daniel Iglesia. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <CoreMotion/CoreMotion.h>
#import <CoreLocation/CoreLocation.h>
#import "PdAudioController.h"
#import "PdBase.h"

#import "Reachability.h"

#import "SettingsViewController.h"
#import "MeControl.h"
#import "PGMidi.h"
#import "VVOSC.h"
#import "LANdiniLANManager.h"

//@class OSCManager, OSCInPort, OSCOutPort;

#import "Audiobus.h"

@interface ViewController : UIViewController<PdReceiverDelegate, UIAccelerometerDelegate,  SettingsViewControllerDelegate, ControlDelegate, UIScrollViewDelegate, AudioSettingsDelegate, PGMidiDelegate, PGMidiSourceDelegate, OSCDelegateProtocol, PdMidiReceiverDelegate, CLLocationManagerDelegate, LANdiniDelegate>{
    
    
    UIButton * settingsButton;
    UILabel* patchOnlyLabel;
    UIView* scrollInnerView;
    
    NSMutableDictionary* allGUIControl;
    UIScrollView* scrollView;
    
    OSCManager *manager;
    OSCInPort *inPort;
    OSCOutPort *outPort;
    
    //LANdini
    OSCInPort* inPortFromLANdini;
    OSCOutPort* outPortToLANdini;
    
    //midi
    PGMidi *midi;
    PGMidiSource *currMidiSource;
    PGMidiDestination *currMidiDestination;
    int currMidiSourceIndex;
    int currMidiDestinationIndex;
	
  BOOL mixingEnabled;
    BOOL isLandscape;
    BOOL isFlipped;
   
    
    void* openPDFile;
    AVCaptureDevice *avCaptureDevice;//for flash
    
    //audio settings
    int samplingRate;
    BOOL inputOrMixing;
     int channelCount;
    
    UINavigationController *navigationController;
    
    //new
    CMMotionManager* motionManager;
    CLLocationManager *locationManager;
    
    LANdiniLANManager* llm;
   
    Reachability* reach;
  int pageCount;
   
}

-(void)connectPorts;
-(void)disconnectPorts;
-(BOOL)isAudioBusConnected;
+(NSString*)fetchSSIDInfo;
+(canvasType)getCanvasType;

@property BOOL backgroundAudioEnabled;
@property (retain) PdAudioController* audioController;
@property (retain) SettingsViewController* settingsVC;
@property (copy, nonatomic) NSString *outputIpAddress;
@property (nonatomic) int inputPortNumber;
@property (nonatomic) int outputPortNumber;

//audio bus - make private
@property (strong, nonatomic) ABAudiobusController *audiobusController;
@property (assign,nonatomic)NSInteger ticks;



@end
