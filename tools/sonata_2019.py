import numpy as np
from PIL import Image, ImageDraw, ImageFilter
S=3; W,H=1400,700
def P(pts): return [(x*S,y*S) for x,y in pts]
def spline(pts, closed=True, n=12):
    pts=list(pts); out=[]
    L=len(pts); rng=range(L) if closed else range(L-1)
    for i in rng:
        p0=pts[(i-1)%L] if closed or i>0 else pts[i]; p1=pts[i]; p2=pts[(i+1)%L]; p3=pts[(i+2)%L] if closed or i+2<L else pts[(i+1)%L]
        for t in np.linspace(0,1,n,endpoint=False):
            t2,t3=t*t,t*t*t
            x=0.5*((2*p1[0])+(-p0[0]+p2[0])*t+(2*p0[0]-5*p1[0]+4*p2[0]-p3[0])*t2+(-p0[0]+3*p1[0]-3*p2[0]+p3[0])*t3)
            y=0.5*((2*p1[1])+(-p0[1]+p2[1])*t+(2*p0[1]-5*p1[1]+4*p2[1]-p3[1])*t2+(-p0[1]+3*p1[1]-3*p2[1]+p3[1])*t3)
            out.append((x,y))
    if not closed: out.append(pts[-1])
    return out
img=Image.new('RGBA',(W*S,H*S),(0,0,0,0))
def mask(poly):
    m=Image.new('L',img.size,0); ImageDraw.Draw(m).polygon(P(poly),fill=255); return m
def ellmask(cx,cy,rx,ry,rot=0):
    m=Image.new('L',img.size,0); ImageDraw.Draw(m).ellipse([ (cx-rx)*S,(cy-ry)*S,(cx+rx)*S,(cy+ry)*S],fill=255); return m
def grad_img(stops, axis='y', a=None, b=None):
    n = img.size[1] if axis=='y' else img.size[0]
    a = 0 if a is None else a*S; b = n if b is None else b*S
    t=np.clip((np.arange(n)-a)/max(1,(b-a)),0,1)
    cols=np.zeros((n,4))
    for i in range(4): cols[:,i]=np.interp(t,[s[0] for s in stops],[ (list(s[1])+[255])[i] if len(s[1])==3 else s[1][i] for s in stops])
    if axis=='y': arr=np.repeat(cols[:,None,:],img.size[0],1)
    else: arr=np.repeat(cols[None,:,:],img.size[1],0)
    return Image.fromarray(arr.astype(np.uint8),'RGBA')
def fill(poly_or_mask, color_or_grad, alpha=1.0):
    global img
    m=poly_or_mask if isinstance(poly_or_mask,Image.Image) else mask(poly_or_mask)
    if alpha<1: m=m.point(lambda v:int(v*alpha))
    src=color_or_grad if isinstance(color_or_grad,Image.Image) else Image.new('RGBA',img.size,tuple(list(color_or_grad)+[255])[:4])
    layer=Image.new('RGBA',img.size,(0,0,0,0)); layer.paste(src,(0,0),m)
    img=Image.alpha_composite(img,layer)
def line(pts,color,w,closed=False):
    global img
    layer=Image.new('RGBA',img.size,(0,0,0,0)); d=ImageDraw.Draw(layer)
    q=P(pts)+([P(pts)[0]] if closed else [])
    d.line(q,fill=color,width=int(w*S),joint='curve'); img=Image.alpha_composite(img,layer)

GROUND=600
# --- shadow
sh=Image.new('L',img.size,0); ImageDraw.Draw(sh).ellipse([90*S,560*S,1360*S,640*S],fill=200); sh=sh.filter(ImageFilter.GaussianBlur(18*S))
fill(sh,(5,8,16))
# --- body silhouette
body=spline([(150,575),(112,520),(108,462),(122,408),(170,384),(300,366),(420,300),(512,244),(735,222),(880,206),(1040,212),(1130,252),(1215,312),(1300,332),(1316,400),(1300,500),(1262,560),(1080,572),(460,580)],n=14)
white=grad_img([(0,(255,255,255)),(0.35,(242,245,249)),(0.62,(214,221,231)),(1,(150,160,176))],'y',300,590)
fill(body,white)
# side reflection band (sky) and lower ground bounce
fill(body,grad_img([(0,(180,205,235,0)),(0.5,(170,195,228,90)),(1,(160,185,220,0))],'y',380,470))
# front face (slightly darker, faces away from key light)
face=spline([(112,520),(108,462),(122,408),(170,388),(300,376),(380,404),(395,470),(385,575),(150,575)],n=10)
fill(face,grad_img([(0,(230,234,240)),(1,(170,178,192))],'y',380,580),0.85)
# hood (lit)
hood=[(124,404),(170,386),(300,366),(560,332),(605,348),(400,406),(300,396)]
fill(spline(hood,n=8),grad_img([(0,(255,255,255)),(1,(236,240,246))],'y',330,405))
line(spline([(124,406),(300,398),(400,408)],closed=False,n=10),(150,160,178,140),2)
# windscreen
ws=[(304,368),(560,334),(728,228),(516,246)]
fill(ws,grad_img([(0,(40,52,74)),(0.6,(18,24,38)),(1,(10,14,24))],'y',230,370))
fill([(380,358),(470,346),(640,250),(560,256)],(255,255,255),0.10)
# side glass
glass=spline([(606,340),(660,300),(742,236),(880,218),(1030,224),(1120,262),(1192,318),(1150,322),(900,328)],n=10)
fill(glass,grad_img([(0,(52,66,92)),(0.5,(22,30,46)),(1,(12,16,26))],'y',218,335))
fill([(760,236),(860,224),(780,320),(690,326)],(255,255,255),0.07)
# B pillar and C pillar
fill([(868,220),(886,220),(880,328),(862,329)],(14,16,22))
fill(spline([(1030,224),(1120,262),(1192,318),(1150,322),(1080,276)],n=8),(16,18,26))
# roof rail chrome hint
line(spline([(742,236),(880,218),(1030,224),(1120,262)],closed=False,n=10),(205,212,222,255),2)
# beltline chrome
line([(610,343),(900,331),(1190,320)],(225,230,238,255),2.4)
# mirror
mir=spline([(588,338),(612,316),(660,312),(668,338),(640,356),(598,356)],n=8)
fill(mir,grad_img([(0,(250,251,253)),(1,(190,198,212))],'y',312,356)); line([(600,350),(662,340)],(30,34,44,255),2)
# door cut lines + handles
line(spline([(700,340),(712,450),(700,560)],closed=False,n=10),(120,130,148,200),1.6)
line(spline([(888,332),(898,450),(892,560)],closed=False,n=10),(120,130,148,200),1.6)
fill(spline([(760,382),(805,379),(806,388),(761,391)],n=4),(170,178,192))
fill(spline([(950,376),(995,373),(996,382),(951,385)],n=4),(170,178,192))
# character lines
line(spline([(400,420),(800,398),(1100,382),(1300,372)],closed=False,n=12),(255,255,255,220),2.2)
line(spline([(400,423),(800,401),(1100,385),(1300,375)],closed=False,n=12),(150,160,178,120),1.4)
line(spline([(610,522),(800,516),(1000,510)],closed=False,n=12),(140,150,168,160),2)
# rocker / lower body darkening and bumper lip
fill(spline([(560,560),(1020,552),(1016,575),(566,584)],n=6),grad_img([(0,(120,130,146)),(1,(40,46,58))],'y',552,584))
fill(spline([(150,575),(380,578),(392,590),(160,592)],n=6),(30,34,42))
# wheel arches
for cx,cy,rx,ry in [(505,548,96,92),(1100,540,90,88)]:
    fill(ellmask(cx,cy,rx,ry),(14,16,22))
# grille (cascading hex)
gr=[(140,440),(332,452),(326,522),(290,560),(170,556),(146,520)]
fill(gr,grad_img([(0,(30,34,44)),(1,(8,10,14))],'y',438,566))
for i in range(8):
    y=452+i*13
    left=150+(i/7)*24; right=326-(i/7)*36
    for j in range(12):
        x=left+4+j*20
        if x+14>right: break
        fill(spline([(x,y),(x+14,y+1),(x+12,y+8),(x-1,y+7)],n=3),(62,68,82))
line(gr,(240,243,248,255),4.2,closed=True)
fill(ellmask(238,463,22,10),(220,226,236)); line(spline([(220,463),(238,455),(256,463),(238,471)],closed=True,n=6),(90,98,112,255),2)
# headlights
far=[(112,414),(150,404),(156,428),(118,438)]
fill(far,(30,38,54)); line([(118,420),(158,410)],(235,245,255,255),2.5)
near=spline([(292,420),(380,412),(520,398),(540,404),(500,424),(390,440),(300,446)],n=8)
fill(near,grad_img([(0,(60,72,96)),(1,(20,26,38))],'y',398,446))
line(spline([(300,428),(400,420),(520,404)],closed=False,n=10),(230,245,255,255),3)
fill(ellmask(340,430,15,9),(215,232,255)); fill(ellmask(398,424,13,8),(215,232,255))
# lower intake and fog/DRL
intake=[(165,566),(370,570),(362,592),(175,590)]
fill(intake,(16,18,24))
line([(340,540),(380,534)],(230,245,255,255),3)
# taillight sliver
fill(spline([(1248,338),(1306,342),(1304,368),(1244,360)],n=6),grad_img([(0,(255,60,80)),(1,(150,10,30))],'x',1240,1310))
# wheels
def wheel(cx,cy,rx,ry):
    fill(ellmask(cx,cy,rx,ry),(10,11,14))
    fill(ellmask(cx+4,cy,rx*0.70,ry*0.72),grad_img([(0,(235,238,242)),(0.5,(160,168,180)),(1,(90,96,108))],'y',cy-ry*0.7,cy+ry*0.7))
    fill(ellmask(cx+4,cy,rx*0.60,ry*0.62),(40,44,52))
    for k in range(10):
        a=k*np.pi/5
        x1=cx+4+np.cos(a)*rx*0.12; y1=cy+np.sin(a)*ry*0.12
        x2=cx+4+np.cos(a)*rx*0.58; y2=cy+np.sin(a)*ry*0.60
        line([(x1,y1),(x2,y2)],(210,215,222,255),5 if k%2==0 else 3)
    fill(ellmask(cx+4,cy,rx*0.14,ry*0.14),(190,196,206)); fill(ellmask(cx+4,cy,rx*0.07,ry*0.07),(60,66,78))
wheel(505,548,78,84); wheel(1100,540,72,80)
# outline crisp edge
out=img.resize((W,H),Image.LANCZOS)
bbox=out.getbbox(); out=out.crop(bbox)
out.save('sonata.png'); out.resize((out.width*1000//out.width, out.height*1000//out.width),Image.LANCZOS).save('sonata_2019_white.webp',quality=90,method=6)
bg=Image.new('RGBA',out.size,(14,22,44,255)); Image.alpha_composite(bg,out).save('prev_dark.png')
bg=Image.new('RGBA',out.size,(225,232,244,255)); Image.alpha_composite(bg,out).save('prev_light.png')
print(out.size)
