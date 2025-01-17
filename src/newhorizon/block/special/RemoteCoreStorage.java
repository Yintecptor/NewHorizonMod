package newhorizon.block.special;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.blocks.storage.StorageBlock;
import mindustry.world.modules.ItemModule;
import newhorizon.func.DrawFuncs;
import newhorizon.interfaces.BeforeLoadc;
import newhorizon.vars.EventTriggers;
import newhorizon.vars.NHWorldVars;

import static mindustry.Vars.tilesize;

public class RemoteCoreStorage extends StorageBlock{
	public static final ObjectMap<Integer, ObjectSet<RemoteCoreStorageBuild>> placedMap = new ObjectMap<>(Team.all.length);
	
	static{
		for(int i = 0; i < Team.all.length; i++){
			placedMap.put(i, ObjectSet.with());
		}
		
		EventTriggers.actBeforeLoad.add(() -> placedMap.each((id, i) -> placedMap.put(id, ObjectSet.with())));
	}
	
	public RemoteCoreStorage(String name){
		super(name);
		update = true;
		hasItems = true;
		itemCapacity = 0;
		configurable = true;
	}
	
	public static int maxPlaceNum(){
		return Mathf.clamp(Vars.world.width() * Vars.world.height() / 10000, 3, 15);
	}
	
	@Override
	public boolean isAccessible(){
		return true;
	}
	
	@Override
	public void setBars(){
		super.setBars();
		bars.add("maxPlace", (RemoteCoreStorageBuild entity) ->
			new Bar(
				() -> "Max Place | " + placedMap.get(entity.team.id).size + " / " + maxPlaceNum(),
				() -> placedMap.get(entity.team.id).size <= maxPlaceNum() ? Pal.heal : Pal.redderDust,
				() -> (float)placedMap.get(entity.team.id).size / maxPlaceNum()
			)
		);
		bars.add("warmup", (RemoteCoreStorageBuild entity) -> new Bar(() -> Mathf.equal(entity.warmup, 1, 0.015f) ? Core.bundle.get("done") : Core.bundle.get("research.load"), () -> Mathf.equal(entity.warmup, 1, 0.015f) ? Pal.heal : Pal.redderDust, () -> entity.warmup));
		bars.remove("items");
	}
	
	@Override
	public boolean canPlaceOn(Tile tile, Team team){
		return super.canPlaceOn(tile, team) && placedMap.get(team.id).size < maxPlaceNum();
	}
	
	public class RemoteCoreStorageBuild extends StorageBuild implements BeforeLoadc{
		public float warmup = 0;
		public float progress = 0;
		
		
		@Override
		public void remove(){
			super.remove();
			NHWorldVars.advancedLoad.remove(this);
		}
		
		
		@Override
		public void add(){
			super.add();
			NHWorldVars.advancedLoad.add(this);
			beforeLoad();
		}
		
		@Override
		public Building init(Tile tile, Team team, boolean shouldAdd, int rotation){
			super.init(tile, team, shouldAdd, rotation);
			
			placedMap.get(team.id).add(this);
//			Log.info("ADD" + placedMap.get(team.id) + " | ID: " + team.id);
			
			return this;
		}
		
		//		@Override
//		public void afterRead(){
//			super.afterRead();
//			placedMap.put(team.id, placedMap.get(team.id) + 1);
//		}
		
		@Override
		public void onRemoved(){
			placedMap.get(team.id).remove(this);
		}
		
		
		@Override
		public void displayBars(Table table){
			super.displayBars(table);
			CoreBlock.CoreBuild b = core();
			if(b != null){
				table.add(new Bar(() -> Core.bundle.format("bar.items", b.items.total()), () -> Pal.items, () -> (float)(b.items.total() / b.storageCapacity))).growX().row();
			}
		}
		
		@Override
		public void updateTile(){
			if(efficiency() > 0 && core() != null && placedMap.get(team.id).size <= maxPlaceNum()){
				if(Mathf.equal(warmup, 1, 0.015F))warmup = 1f;
				else warmup = Mathf.lerpDelta(warmup, 1, 0.01f);
			}else{
				if(Mathf.equal(warmup, 0, 0.015F))warmup = 0f;
				else warmup = Mathf.lerpDelta(warmup, 0, 0.03f);
			}
			
			progress += warmup * efficiency() * Time.delta;
			
			if(Mathf.equal(warmup, 1, 0.015F)){
				if(linkedCore == null){
					linkedCore = core();
					items = linkedCore.items;
				}
			}else{
				linkedCore = null;
				items = new ItemModule();
			}
		}
		
		@Override
		public void write(Writes write){
			super.write(write);
			write.f(warmup);
		}
		
		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			warmup = read.f();
		}
		
		@Override
		public void draw(){
			super.draw();
			
			Draw.z(Layer.effect - 1f);
			Draw.color(team.color);
			Building b = core();
			if(b != null){
				for (int i = 0; i < 5; i++) {
					float f = (progress - 25 * i) % 100 / 100;
					Tmp.v1.trns(angleTo(b), f * tilesize * size * 4);
					Lines.stroke(warmup * 1.5f * (1 - f));
					Lines.square(x + Tmp.v1.x, y + Tmp.v1.y, (1 - f) * 8, 45);
				}
			}
		}
		
		@Override
		public void drawSelect(){ }
		
		@Override
		public void drawConfigure(){
			if(core() != null)DrawFuncs.posSquareLink(Mathf.equal(warmup, 1, 0.015f) ? Pal.heal : Pal.redderDust, 1, 4, true, tile, core());
		}
		
		@Override
		public void beforeLoad(){
			placedMap.get(team.id).add(this);
		}
	}
}
