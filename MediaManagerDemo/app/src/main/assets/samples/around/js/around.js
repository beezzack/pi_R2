// 測試多個目標+播放動畫-單一tracker-同物件不同目標重複出現
// 出現的東西是照片
// 確認版
var World = {
    loaded: false,
    dinoSettings: {
        diplodocus: {scale: 0.4},
        spinosaurus: {scale: 0.004},
        triceratops: {scale: 0.4},
        tyrannosaurus: {scale: 0.4}
    },

    init: function initFn() {
        this.createOverlays();
    },

    createOverlays: function () {
        /*
         First an AR.ImageTracker needs to be created in order to start the recognition engine. It is initialized with a AR.TargetCollectionResource specific to the target collection that should be used. Optional parameters are passed as object in the last argument. In this case a callback function for the onTargetsLoaded trigger is set. Once the tracker loaded all its target images, the function worldLoaded() is called.

         Important: If you replace the tracker file with your own, make sure to change the target name accordingly.
         Use a specific target name to respond only to a certain target or use a wildcard to respond to any or a certain group of targets.
         */
        var targetCollectionResource = new AR.TargetCollectionResource("assets/tracker.wtc");

        /*
         To enable simultaneous tracking of multiple targets 'maximumNumberOfConcurrentlyTrackableTargets' has to be defined.
         */
        var tracker = new AR.ImageTracker(targetCollectionResource, {
            maximumNumberOfConcurrentlyTrackableTargets: 5, // a maximum of 5 targets can be tracked simultaneously
            /*
             Disables extended range recognition.
             The reason for this is that extended range recognition requires more processing power and with multiple targets
             the SDK is trying to recognize targets until the maximumNumberOfConcurrentlyTrackableTargets is reached and it
             may slow down the tracking of already recognized targets.
             */
            extendedRangeRecognition: AR.CONST.IMAGE_RECOGNITION_RANGE_EXTENSION.OFF,
            // onTargetsLoaded: this.worldLoaded,
            onError: function (errorMessage) {
                alert(errorMessage);
            }
        });

        /*
         Pre-load models such that they are available in cache to avoid any
         initial slowdown upon first recognition.
         */
        // new AR.Model("assets/models/diplodocus.wt3");
        // new AR.Model("assets/models/spinosaurus.wt3");
        // new AR.Model("assets/models/triceratops.wt3");
        // new AR.Model("assets/models/tyrannosaurus.wt3");
        var b = 0;var c = 0;var e = 0;var k = 0;var r = 0;var s = 0;var t = 0;var w = 0;
        var temp="a";var temp2="a";

        new AR.ImageTrackable(tracker, "*", {
            onImageRecognized: function (target) {
                /*
                 Create 3D model based on which target was recognized.
                 */
//                var model = new AR.Model("assets/models/" + target.name + ".wt3", {
//                var temp = target.name[0];
//                if(temp==0){
                switch(target.name[0]){
                    case "b":
                        temp=b;
                        b=1;
                        break;
                    case "c":
                        temp=c;
                        c=1
                        break;
                    case "e":
                        temp=e;
                        e=1;
                        break;
                    case "k":
                        temp=k;
                        k=1;
                        break;
                    case "r":
                        temp=r;
                        r=1;
                        break;
                    case "s":
                        temp=s;
                        s=1;
                        break;
                    case "t":
                        temp=t;
                        t=1;
                        break;
                    case "w":
                        temp=w;
                        w=1;
                        break;
                }
                if(temp==0){
                    var img = new AR.ImageResource("assets/models/"+target.name[0]+".png");
                    if(target.name[0]=="b"||target.name[0]=="e"||target.name[0]=="c"||target.name[0]=="s"){
                        var overlay = new AR.ImageDrawable(img,1,{
                            scale:{
                                x:0.25,
                                y:0.25,
                                x:0.25
                            },
                            translate:{
                                y:0.45
                            }
                        });
                    }
//                    else if(target.name[0]=="k"){
//                        var overlay = new AR.ImageDrawable(img,1,{
//                            scale:{
//                                x:0.25,
//                                y:0,
//                                x:0.25
//                            },
//                            translate:{
//                                y:0.45
//                            }
//                        });
//                    }
                    else if(target.name[0]=="t"||target.name[0]=="k"){
                        var overlay = new AR.ImageDrawable(img,1,{
                            scale:{
                                x:0.25,
                                y:0,
                                x:0.25
                            },
                            translate:{
                                y:0.45
                            }
                        });
                    }
                    else{
                        var overlay = new AR.ImageDrawable(img,1,{
                            scale:{
                                x:0.25,
                                y:0.25,
                                x:0.25
                            },
                            translate:{
                                y:0.25
                            }

                        });
                    }
                }
//                }


                // var model = new AR.Model("assets/models/diplodocus.wt3", {
                //     scale: World.dinoSettings["diplodocus"].scale,
                //     rotate: {
                //         z: 180
                //     }
                // });

                /*
                 Adds the model as augmentation for the currently recognized target.
                 */
                this.addImageTargetCamDrawables(target, overlay);
                // this.animationplay = new AR.ModelAnimation(model, "Attack");
                // this.animationplay.start(50);

                if(!World.loaded){
                    World.loaded = true;
                }
            },
            onError: function (errorMessage) {
                alert(errorMessage);
            },
            onImageLost: function(target){
                var temp2 = target.name[0];
                switch(temp2){
                    case "b":
                        b=0;
                        break;
                    case "c":
                        c=0;
                        break;
                    case "e":
                        e=0;
                        break;
                    case "k":
                        k=0;
                        break;
                    case "r":
                        r=0;
                        break;
                    case "s":
                        s=0;
                        break;
                    case "e":
                        e=0;
                        break;
                    case "w":
                        w=0;
                        break;
                }
            }
        });
    }

//    worldLoaded: function worldLoadedFn() {
//        var cssDivInstructions = " style='display: table-cell;vertical-align: middle; text-align: right; width: 50%; padding-right: 15px;'";
//        var cssDivTyrannosaurus = " style='display: table-cell;vertical-align: middle; text-align: left; padding-right: 15px; width: 38px'";
//        var cssDivTriceratops = " style='display: table-cell;vertical-align: middle; text-align: left; padding-right: 15px; width: 38px'";
//        var cssDivSpinosaurus = " style='display: table-cell;vertical-align: middle; text-align: left; padding-right: 15px; width: 38px'";
//        var cssDivDiplodocus = " style='display: table-cell;vertical-align: middle; text-align: left; padding-right: 15px;'";
//
//        document.getElementById('loadingMessage').innerHTML =
//            "<div" + cssDivInstructions + ">Scan one of the dinosaur targets:</div>" +
//            "<div" + cssDivTyrannosaurus + "><img src='assets/tyrannosaurus.png'></div>" +
//            "<div" + cssDivTriceratops + "><img src='assets/triceratops.png'></div>" +
//            "<div" + cssDivSpinosaurus + "><img src='assets/spinosaurus.png'></div>" +
//            "<div" + cssDivDiplodocus + "><img src='assets/diplodocus.png'></div>";
//    }
};

World.init();
