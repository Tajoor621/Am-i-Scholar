"use strict";
/* Cross-platform encrypted backup envelope: PBKDF2-HMAC-SHA-256 + AES-256-GCM. */
(function(root){
 const FORMAT="am-i-scholar-encrypted",VERSION=1,ITERATIONS=310000,MAX_BYTES=10*1024*1024;
 const subtle=()=>{if(!root.crypto?.subtle)throw Error("Secure encryption is unavailable in this browser.");return root.crypto.subtle};
 const encoder=()=>new TextEncoder(),decoder=()=>new TextDecoder("utf-8",{fatal:true});
 const b64=bytes=>{let s="";for(const b of bytes)s+=String.fromCharCode(b);return btoa(s)};
 const un64=value=>{if(typeof value!=="string"||value.length>MAX_BYTES*2)throw Error("Invalid encrypted backup encoding");const s=atob(value);return Uint8Array.from(s,c=>c.charCodeAt(0))};
 const header=e=>JSON.stringify({format:e.format,version:e.version,kdf:e.kdf,iterations:e.iterations,cipher:e.cipher,salt:e.salt,iv:e.iv});
 async function keyFor(passphrase,salt,usage){
  if(typeof passphrase!=="string"||passphrase.length<12||passphrase.length>1024)throw Error("Use a passphrase with at least 12 characters.");
  const material=await subtle().importKey("raw",encoder().encode(passphrase),"PBKDF2",false,["deriveKey"]);
  return subtle().deriveKey({name:"PBKDF2",hash:"SHA-256",salt,iterations:ITERATIONS},material,{name:"AES-GCM",length:256},false,[usage]);
 }
 async function encrypt(payload,passphrase){
  const salt=root.crypto.getRandomValues(new Uint8Array(16)),iv=root.crypto.getRandomValues(new Uint8Array(12));
  const envelope={format:FORMAT,version:VERSION,kdf:"PBKDF2-HMAC-SHA-256",iterations:ITERATIONS,cipher:"AES-256-GCM",salt:b64(salt),iv:b64(iv)};
  const key=await keyFor(passphrase,salt,"encrypt"),plain=encoder().encode(JSON.stringify(payload));
  const ciphertext=new Uint8Array(await subtle().encrypt({name:"AES-GCM",iv,additionalData:encoder().encode(header(envelope)),tagLength:128},key,plain));
  envelope.ciphertext=b64(ciphertext);
  const output=JSON.stringify(envelope,null,2);if(encoder().encode(output).length>MAX_BYTES)throw Error("Encrypted backup exceeds 10 MB.");return output;
 }
 async function decrypt(text,passphrase){
  if(typeof text!=="string"||encoder().encode(text).length>MAX_BYTES)throw Error("Encrypted backup exceeds 10 MB.");
  let e;try{e=JSON.parse(text)}catch{throw Error("Invalid encrypted backup JSON.");}
  if(!e||e.format!==FORMAT||e.version!==VERSION)throw Error("Unsupported encrypted backup format.");
  const fields=["format","version","kdf","iterations","cipher","salt","iv","ciphertext"];if(Object.keys(e).length!==fields.length||fields.some(key=>!Object.prototype.hasOwnProperty.call(e,key)))throw Error("Invalid encrypted backup envelope.");
  if(e.kdf!=="PBKDF2-HMAC-SHA-256"||e.iterations!==ITERATIONS||e.cipher!=="AES-256-GCM")throw Error("Unsupported encryption parameters.");
  const salt=un64(e.salt),iv=un64(e.iv),ciphertext=un64(e.ciphertext);
  if(salt.length!==16||iv.length!==12||ciphertext.length<16||ciphertext.length>MAX_BYTES)throw Error("Invalid encrypted backup fields.");
  try{const key=await keyFor(passphrase,salt,"decrypt"),plain=await subtle().decrypt({name:"AES-GCM",iv,additionalData:encoder().encode(header(e)),tagLength:128},key,ciphertext);return JSON.parse(decoder().decode(plain))}
  catch(error){if(error instanceof TypeError||error instanceof SyntaxError)throw error;throw Error("Could not decrypt backup. Check the passphrase and file integrity.");}
 }
 const api={FORMAT,VERSION,ITERATIONS,encrypt,decrypt};
 if(typeof module!=="undefined"&&module.exports)module.exports=api;
 if(root)root.AmIScholarBackupCrypto=api;
})(typeof globalThis!=="undefined"?globalThis:this);
