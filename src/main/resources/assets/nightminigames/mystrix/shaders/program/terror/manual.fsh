#version 150
uniform float TimeSeconds;
in vec2 texCoord;
in vec4 effectData;
out vec4 fragColor;
float hash2(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
float noise2(vec2 p){vec2 i=floor(p);vec2 f=fract(p);f=f*f*(3.0-2.0*f);return mix(mix(hash2(i),hash2(i+vec2(1.0,0.0)),f.x),mix(hash2(i+vec2(0.0,1.0)),hash2(i+vec2(1.0)),f.x),f.y);}
float capsule(vec2 p,vec2 a,vec2 b,float r){vec2 q=p-a;vec2 d=b-a;return length(q-d*clamp(dot(q,d)/dot(d,d),0.0,1.0))-r;}
float ellipse(vec2 p,vec2 r){return (length(p/r)-1.0)*min(r.x,r.y);}
float human(vec2 p){
    float d=ellipse(p-vec2(0.0,2.53),vec2(0.19,0.26));
    d=min(d,capsule(p,vec2(0.0,2.06),vec2(0.0,2.33),0.09));
    d=min(d,capsule(p,vec2(0.0,1.32),vec2(0.0,2.04),0.23));
    d=min(d,capsule(p,vec2(-0.21,1.97),vec2(-0.39,1.04),0.085));
    d=min(d,capsule(p,vec2(0.21,1.97),vec2(0.39,1.04),0.085));
    d=min(d,capsule(p,vec2(-0.10,1.30),vec2(-0.17,0.13),0.10));
    return min(d,capsule(p,vec2(0.10,1.30),vec2(0.17,0.13),0.10));
}
void main(){
    int mode=int(floor(effectData.r*255.0+0.5));
    float time=TimeSeconds;
    if(mode>=10){
        float grain=noise2(texCoord*vec2(140.0,90.0));
        float shade=effectData.g;
        vec3 skin=vec3(0.075,0.079,0.072)*(0.33+shade*0.67);
        skin*=0.68+grain*0.40;
        float pores=noise2(texCoord*vec2(48.0,33.0));
        skin*=0.78+0.22*pores;
        if(mode==10){
            vec2 face=vec2((texCoord.x-.75)*6.283185,(texCoord.y-.5)*3.141593);
            float front=1.0-smoothstep(.9,1.3,abs(face.x));
            float eyes=min(ellipse(face-vec2(-.36,.14),vec2(.23,.17)),ellipse(face-vec2(.36,.11),vec2(.23,.18)));
            float socket=1.0-smoothstep(-.03,.09,eyes);
            float mouth=1.0-smoothstep(-.04,.045,ellipse(face-vec2(.015,-.43),vec2(.23,.34)));
            float nose=1.0-smoothstep(.01,.10,abs(face.x-.02));
            skin=mix(skin,vec3(.002,.003,.003),front*max(socket,mouth));
            skin+=vec3(.014,.015,.012)*nose*front*(1.0-smoothstep(.08,.32,abs(face.y+.10)));
            // Tiny dull eye reflections: no emitted light and no bright white face.
            float glint=(1.0-smoothstep(.015,.045,length(face-vec2(-.35,.13))))+(1.0-smoothstep(.015,.045,length(face-vec2(.35,.10))));
            skin+=vec3(.09,.095,.065)*glint*front;
            float crease=exp(-abs(face.y+.18+abs(face.x)*.30)*45.0);
            skin*=1.0-.35*crease*front;
        }else if(mode==11){
            float folds=.68+.32*sin(texCoord.x*75.4)*sin(texCoord.x*75.4);
            float stains=noise2(texCoord*vec2(22.0,7.0));
            skin=vec3(.064,.069,.065)*(.35+.65*shade)*folds*(.55+.45*stains);
        }else if(mode==12){
            float ribs=pow(.5+.5*sin(texCoord.y*93.0),8.0);
            skin*=.79+.21*ribs;
        }else if(mode==13){skin=vec3(.004,.006,.007)*(.4+.6*shade);}
        fragColor=vec4(skin,effectData.a);return;
    }
    vec2 p=vec2((texCoord.x-0.5)*1.6,texCoord.y*3.0);
    float n=noise2(p*6.0+vec2(time*0.12,-time*0.09));
    float d=human(p);
    if(mode==3){d=min(ellipse(p-vec2(0.0,1.78),vec2(0.34,0.54)),capsule(p,vec2(-0.49,0.78),vec2(0.49,0.78),0.23));}
    if(mode==4){p.x+=(noise2(p*2.7+time*0.13)-0.5)*0.24;d=human(p)+(n-0.5)*0.18;}
    float feather=mode==4?0.13:0.035;
    float mask=1.0-smoothstep(-feather,feather,d+(n-0.5)*0.018);
    float halo=1.0-smoothstep(0.015,mode==4?0.18:0.065,abs(d));
    float visibility=mix(0.90,0.43,effectData.g);
    if(mode==2){visibility=0.88;halo=0.0;}
    float density=mode==4?0.45+0.40*noise2(p*3.0+vec2(0.0,time*0.20)):0.91+0.07*n;
    float alpha=mask*density*visibility*effectData.a;
    if(alpha<0.004)discard;
    vec3 ink=vec3(0.007,0.009,0.014)+vec3(0.07,0.085,0.11)*halo;
    if(mode==4)ink+=vec3(0.025,0.030,0.038)*n;
    fragColor=vec4(ink,alpha);
}
