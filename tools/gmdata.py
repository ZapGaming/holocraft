import struct
class GM:
    def __init__(s,path):
        s.d=open(path,'rb').read(); s.ch={}
        p=8
        while p<len(s.d):
            n=s.d[p:p+4].decode(); sz=struct.unpack_from('<I',s.d,p+4)[0]; s.ch[n]=(p+8,sz); p+=8+sz
    def u32(s,o): return struct.unpack_from('<I',s.d,o)[0]
    def i32(s,o): return struct.unpack_from('<i',s.d,o)[0]
    def str_at(s,o):
        if o==0: return None
        n=s.u32(o-4); return s.d[o:o+n].decode('utf8','replace')
    def plist(s,o):
        n=s.u32(o); return list(struct.unpack_from('<%dI'%n,s.d,o+4))
    def sprites(s):
        o,_=s.ch['SPRT']; out={}
        for p in s.plist(o):
            name=s.str_at(s.u32(p)); w,h=s.u32(p+4),s.u32(p+8)
            ox,oy=s.i32(p+0x30),s.i32(p+0x34)
            q=p+0x38
            if s.i32(q)==-1:
                ver=s.u32(q+4); stype=s.u32(q+8); q+=0x14
                if ver>=2: q+=4
                if ver>=3: q+=4
            frames=s.plist(q) if stype==0 else []
            out[name]=dict(w=w,h=h,ox=ox,oy=oy,frames=frames)
        return out
    def tpag(s,p):
        v=struct.unpack_from('<11H',s.d,p)
        return dict(sx=v[0],sy=v[1],sw=v[2],sh=v[3],tx=v[4],ty=v[5],tw=v[6],th=v[7],bw=v[8],bh=v[9],tex=v[10])
    def strings(s):
        o,_=s.ch['STRG']; return [s.str_at(p+4) for p in s.plist(o)]
