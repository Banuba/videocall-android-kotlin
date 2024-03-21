function Effect() {
    var self = this;

    this.waitHint = function() {
        if((new Date()).getTime() > self.t) {
            Api.hideHint();
            self.faceActions = [];
        }
    };

    this.init = function() {
        if(Api.getPlatform() == "ios" || Api.getPlatform() == "iOS" || Api.getPlatform() == "macOS") {
            Api.showHint("Camera 360");
            self.t = (new Date()).getTime() + 5000;
            self.faceActions = [self.waitHint];
        }

        Api.meshfxMsg("spawn", 3, 0, "tri.bsm2");
        Api.meshfxMsg("spawn", 2, 0, "!glfx_FACE");
        Api.meshfxMsg("spawn", 0, 0, "CubemapEverest.bsm2");
        Api.meshfxMsg("spawn", 1, 0, "CubemapEverestMorph.bsm2");
        Api.meshfxMsg("spawn", 4, 0, "plane.bsm2");
        Api.playVideo("frx", true, 1)
        Api.playSound("Cubemap_Everest_L_Channel.ogg",true,1);
        Api.showRecordButton();
    };

    this.restart = function() {
        Api.meshfxReset();
        Api.stopSound("Cubemap_Everest_L_Channel.ogg");
        self.init();
    };

    this.faceActions = [];
    this.noFaceActions = [];

    this.videoRecordStartActions = [];
    this.videoRecordFinishActions = [];
    this.videoRecordDiscardActions = [this.restart];
}

configure(new Effect());