//
//  SettingsViewController.h
//  MobMuPlat
//
//  Created by Daniel Iglesia on 11/30/12.
//  Copyright (c) 2012 Daniel Iglesia. All rights reserved.
//

#import <UIKit/UIKit.h>


#import "PGMidi.h"
#import "MeSlider.h"
#import <MediaPlayer/MediaPlayer.h>
#import "LANdiniLANManager.h"
#import "Reachability.h"

@protocol SettingsViewControllerDelegate;
@protocol AudioSettingsDelegate;
@protocol LANdiniDelegate;

typedef enum{
    canvasTypeWidePhone = 0,
    canvasTypeTallPhone = 1,
    canvasTypeWideTablet = 2,
    canvasTypeTallTablet
} canvasType;

@interface SettingsViewController : UIViewController<UITableViewDataSource, UITableViewDelegate, LANdiniUserStateDelegate, UITextFieldDelegate>{
    canvasType hardwareCanvasType;
    
    NSMutableArray *MMPFiles, *allFiles;
    BOOL mmpOrAll;

    NSString* consoleTextString;
    
    NSMutableArray* consoleStringQueue;
    
    //views
    
    int rateValueArray[6];
    int requestedBlockCount;
    
    MPVolumeView *audioRouteView;
    int outputChannelCount;

}

@property (nonatomic, strong) IBOutlet UIButton* consoleViewButton;
@property (nonatomic, strong) IBOutlet UIButton* documentViewButton;
@property (nonatomic, strong) IBOutlet UIButton* audioMidiViewButton;
@property (nonatomic, strong) IBOutlet UIButton* networkViewButton;

@property (nonatomic, strong) IBOutlet UIView* consoleView;
@property (nonatomic, strong) IBOutlet UIView* documentView;
@property (nonatomic, strong) IBOutlet UIView* networkView;
@property (nonatomic, strong) IBOutlet UIScrollView* audioMidiScrollView;

@property (nonatomic, strong) IBOutlet UITableView* documentsTableView;
@property (nonatomic, strong) IBOutlet UIButton* showFilesButton;
@property (nonatomic, strong) IBOutlet UIButton* flipInterfaceButton;

@property (nonatomic, strong) IBOutlet UITextView* consoleTextView;
@property (nonatomic, strong) IBOutlet UIButton* clearConsoleButton;

//audio midi
@property (nonatomic, strong) IBOutlet UIView* audioMidiContentView;
@property (nonatomic, strong) IBOutlet UITableView* midiSourceTableView;
@property (nonatomic, strong) IBOutlet UITableView* midiDestinationTableView;
@property (nonatomic, strong) IBOutlet UISegmentedControl* tickSeg;
@property (nonatomic, strong) IBOutlet UILabel* tickValueLabel;
@property (nonatomic, strong) IBOutlet UISegmentedControl* rateSeg;
@property (nonatomic, strong) IBOutlet UIButton* audioEnableButton;
@property (nonatomic, strong) IBOutlet UISwitch* audioInputSwitch;
@property (nonatomic, strong) IBOutlet UIView* audioRouteContainerView;
@property (nonatomic, strong) IBOutlet UILabel* audioRouteLabel;

//network view
@property (nonatomic, strong) IBOutlet UISegmentedControl* networkTypeSeg;
@property (nonatomic, strong) IBOutlet UIView *networkingSubView;

@property (nonatomic, strong) IBOutlet UIView *LANdiniSubView;
@property (nonatomic, strong) IBOutlet UIView *multiDirectConnectionSubView;

@property (nonatomic, strong) IBOutlet UITextField *ipAddressTextField;
@property (nonatomic, strong) IBOutlet UIButton *ipAddressResetButton;
@property (nonatomic, strong) IBOutlet UITextField *outputPortNumberTextField;
@property (nonatomic, strong) IBOutlet UITextField *inputPortNumberTextField;

@property (nonatomic, strong) IBOutlet UILabel* LANdiniNetworkLabel;
@property (nonatomic, strong) IBOutlet UISwitch* LANdiniEnableSwitch;
@property (nonatomic, strong) IBOutlet UILabel* LANdiniTimeLabel;
@property (nonatomic, strong) IBOutlet UITableView* LANdiniUserTableView;



@property (nonatomic, assign) id <SettingsViewControllerDelegate> delegate;
@property (nonatomic, assign) id <AudioSettingsDelegate> audioDelegate;
@property (nonatomic, assign) id <LANdiniDelegate> LANdiniDelegate;

-(void)reloadFileTable;
-(void)consolePrint:(NSString*)message;
-(void)reloadMidiSources;

@end

@protocol SettingsViewControllerDelegate <NSObject>
- (void)settingsViewControllerDidFinish:(SettingsViewController *)controller;
- (BOOL)loadScene:(NSDictionary*)sceneDict;
- (BOOL)loadScenePatchOnly:(NSString*)filename;
- (void)flipInterface;
@property(copy, nonatomic) NSString *outputIpAddress;
@property(nonatomic)int inputPortNumber;
@property(nonatomic)int outputPortNumber;

@end

@protocol AudioSettingsDelegate <NSObject>//audio+midi stuff stuff
-(int)blockSize;
-(int)setTicksPerBuffer:(int)newTick;//returns actual ticks
-(int)actualTicksPerBuffer;
-(int)setRate:(int)inRate;
-(int)sampleRate;
-(PGMidi*) midi;
-(void)setMidiSourceIndex:(int)index;
-(void)setMidiDestinationIndex:(int)index;
-(int)setChannelCount:(int)newChannelCount;
- (void)setAudioInputEnabled:(BOOL)enabled; //for mic input vs vibration
-(BOOL)isAudioBusConnected;
@property BOOL backgroundAudioEnabled;

@end

@protocol LANdiniDelegate <NSObject>
-(float)getLANdiniTime;
-(void)enableLANdini:(BOOL)enable;
-(Reachability*)getReachability;
@end
