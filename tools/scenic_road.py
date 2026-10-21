import numpy as np
from PIL import Image, ImageDraw, ImageFilter
rng=np.random.default_rng(11)
S=2; W,H=900*S,1000*S
HOR=int(H*0.50)
yy,xx=np.mgrid[0:H,0:W].astype(np.float32)
def lerpc(stops,t):
    t=np.clip(t,0,1); out=np.zeros(t.shape+(3,),np.float32)
    for i in range(3): out[...,i]=np.interp(t,[s[0] for s in stops],[s[1][i] for s in stops])
    return out
canvas=lerpc([(0,(6,10,30)),(.30,(16,26,70)),(.55,(58,52,112)),(.74,(170,96,110)),(.88,(246,150,96)),(1,(255,206,140))],yy/HOR)
sx,sy=W*0.58,HOR-6*S
d=np.sqrt((xx-sx)**2+((yy-sy)*1.8)**2)
canvas+=np.exp(-(d/(W*0.16))**2)[...,None]*np.array([255,200,140])*0.9
canvas+=np.exp(-(d/(W*0.45))**2)[...,None]*np.array([255,150,100])*0.25
def fbm(n,oct,seed,rough=1.9):
    r=np.random.default_rng(seed); out=np.zeros(n)
    for o in range(oct):
        k=2**(o+1); pts=r.uniform(-1,1,k+2)
        xs=np.linspace(0,k,n); i=np.floor(xs).astype(int); f=xs-i; f=f*f*(3-2*f)
        out+=(pts[i]*(1-f)+pts[i+1]*f)/(rough**o)
    return out
def layer(top,shade_top,shade_bot,rim=None,rimw=3):
    global canvas
    m=(yy>=top[None,:].astype(np.float32))
    depth=np.clip((yy-top[None,:])/(H*0.25),0,1)
    col=lerpc([(0,shade_top),(1,shade_bot)],depth)
    canvas=np.where(m[...,None],col,canvas)
    if rim is not None:
        band=np.exp(-((yy-top[None,:])/(rimw*S))**2)*m
        canvas+=band[...,None]*np.array(rim)
x=np.arange(W)
far=HOR-95*S+ 85*S*fbm(W,7,3)-40*S*np.exp(-((x-W*0.75)/(W*0.12))**2)
layer(far,(118,88,128),(70,58,104),rim=(120,70,40),rimw=4)
mid=HOR-35*S+55*S*fbm(W,7,8,1.8)
mid=np.where(x<W*0.5,mid,np.minimum(mid,HOR-5*S+40*S*fbm(W,6,9)))
layer(mid,(74,58,96),(40,34,66),rim=(90,50,30),rimw=3)
near=HOR+4*S+14*S*fbm(W,6,21)
layer(near,(36,30,52),(26,24,40))
# left cliff with lit face
cl=np.full(W,H*2.0)
t=x/(W*0.46)
cl=np.where(x<W*0.46,HOR+30*S-470*S*np.clip(1-t,0,1)**1.45+22*S*fbm(W,6,33),cl)
m=yy>=cl[None,:]
face=np.clip((xx/(W*0.46)),0,1)
ccol=lerpc([(0,(30,20,30)),(.6,(70,42,44)),(1,(120,70,56))],face*(1-np.clip((yy-cl[None,:])/(H*0.5),0,1))*1.0)
canvas=np.where(m[...,None],ccol,canvas)
band=np.exp(-((yy-cl[None,:])/(5*S))**2)*m
canvas+=band[...,None]*np.array([200,110,60])*np.clip(1-t,0,1)[None,:,None]
# ground under horizon
g=(yy>=near[None,:]+2)&(yy>HOR)
gcol=lerpc([(0,(48,36,48)),(.15,(28,24,38)),(1,(9,10,18))],(yy-HOR)/(H-HOR))
canvas=np.where((g&~m)[...,None],gcol,canvas)
canvas=np.clip(canvas,0,255)
im=Image.fromarray(canvas.astype(np.uint8)); dr=ImageDraw.Draw(im,'RGBA')
for _ in range(110):
    px=rng.uniform(0,W); py=rng.uniform(0,HOR*0.38); a=int(rng.uniform(40,170)); r=rng.uniform(.6,1.5)*S
    dr.ellipse([px-r,py-r,px+r,py+r],fill=(255,255,255,a))
N=240
def road_at(t):
    cx=W*0.50+W*0.13*np.sin(t*np.pi*0.85)*t**0.9
    y=H-(H-HOR-6*S)*(1-(1-t)**2.3)
    half=W*0.42*(1-t)**1.3+2*S
    return cx,y,half
Lp=[];Rp=[]
for i in range(N+1):
    tt=i/N*0.996; cx,y,h=road_at(tt); Lp.append((cx-h,y)); Rp.append((cx+h,y))
dr.polygon(Lp+Rp[::-1],fill=(40,42,56,255))
for i in range(N):
    tt=i/N; cx,y,h=road_at(tt)
    dr.line([(cx-h,y),(cx+h,y)],fill=(255,170,110,int(110*tt**4)),width=2*S)
    dr.line([(cx-h*0.25,y),(cx+h*0.25,y)],fill=(255,255,255,int(10*(1-tt))),width=2*S)
for side in(-1,1):
    pts=[road_at(i/N*0.99) for i in range(N+1)]
    dr.line([(c+side*h*0.92,y) for c,y,h in pts],fill=(235,235,240,190),width=3*S)
for k in range(16):
    t0=(k/16)*0.975; t1=t0+0.024
    pts=[road_at(t0+(t1-t0)*i/7) for i in range(8)]
    w=max(1,int(8*S*(1-t0)**1.4))
    dr.line([(c,y) for c,y,h in pts],fill=(252,206,110,235),width=w)
for _ in range(220):
    tt=rng.uniform(0,0.92); cx,y,h=road_at(tt); side=rng.choice([-1,1])
    px=cx+side*(h*1.08+rng.uniform(0,W*0.3*(1-tt)+8)); r=rng.uniform(2,12)*S*(1-tt)+S
    c=int(rng.uniform(12,30))
    dr.ellipse([px-r*1.7,y-r,px+r*1.7,y+r*0.3],fill=(c+10,c+6,c+14,255))
for _ in range(120):
    px=rng.uniform(W*0.38,W); py=HOR+rng.uniform(-2,22)*S; h=rng.uniform(5,16)*S
    dr.polygon([(px,py-h),(px-h*0.28,py),(px+h*0.28,py)],fill=(16,15,26,255))
im=im.filter(ImageFilter.GaussianBlur(0.7*S))
v=np.asarray(im).astype(np.float32)
vig=1-0.40*(((xx-W/2)/(W/2))**2*0.5+((yy-H*0.55)/(H/2))**2*0.5)
v*=np.clip(vig,0.5,1)[...,None]
im=Image.fromarray(np.clip(v,0,255).astype(np.uint8)).resize((900,1000),Image.LANCZOS)
im.save('scenic_road.webp',quality=84,method=6); im.save('scenic_preview.png')
import os; print(os.path.getsize('scenic_road.webp'))
